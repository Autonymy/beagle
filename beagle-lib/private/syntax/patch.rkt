#lang racket/base

(require racket/list
         racket/string
         "tokenize.rkt"
         "cst.rkt")

(provide
 cst-replace-form
 cst-insert-form-after
 cst-delete-form
 cst-wrap-form
 cst-rename-symbol
 cst-add-binding
 cst-add-map-entry
 text->cst-nodes)

;; ---------------------------------------------------------------------------
;; Parse text into CST nodes (for insertion/replacement)
;; ---------------------------------------------------------------------------

(define (text->cst-nodes text)
  (cst-root-children (build-cst (tokenize text))))

;; ---------------------------------------------------------------------------
;; Core tree modification
;; ---------------------------------------------------------------------------

(define (list-replace-at lst idx new-val)
  (for/list ([item (in-list lst)] [i (in-naturals)])
    (if (= i idx) new-val item)))

(define (list-splice lst idx remove-count insert-items)
  (append (take lst idx)
          insert-items
          (drop lst (+ idx remove-count))))

(define (cst-modify-at root path operation)
  (cond
    [(null? path) (error 'cst-modify-at "empty path")]
    [(null? (cdr path))
     (define children (container-children root))
     (define ai (content-idx->actual-idx children (car path)))
     (unless ai (error 'cst-modify-at "content index ~a out of range" (car path)))
     (update-container root (operation children ai))]
    [else
     (define children (container-children root))
     (define ai (content-idx->actual-idx children (car path)))
     (unless ai (error 'cst-modify-at "content index ~a out of range" (car path)))
     (define child (list-ref children ai))
     (define modified (cst-modify-at child (cdr path) operation))
     (update-container root (list-replace-at children ai modified))]))

;; ---------------------------------------------------------------------------
;; Public operations
;; ---------------------------------------------------------------------------

(define (cst-replace-form root path new-text)
  (define new-nodes (text->cst-nodes new-text))
  (define replacement
    (cond
      [(= (length new-nodes) 1) (car new-nodes)]
      [else (error 'cst-replace-form "replacement must be a single form, got ~a" (length new-nodes))]))
  (cst-modify-at root path
    (lambda (children ai)
      (list-replace-at children ai replacement))))

(define (cst-insert-form-after root path new-text)
  (define new-nodes (text->cst-nodes new-text))
  (cst-modify-at root path
    (lambda (children ai)
      (define ws (cst-ws (token 'newline "\n" 0 0 0)))
      (list-splice children (add1 ai) 0 (cons ws new-nodes)))))

(define (cst-delete-form root path)
  (cst-modify-at root path
    (lambda (children ai)
      ;; Remove node + any trailing whitespace (up to one newline)
      (define before (take children ai))
      (define after (drop children (add1 ai)))
      (define trimmed
        (let loop ([cs after] [removed-ws? #f])
          (cond
            [(and (not removed-ws?) (pair? cs) (cst-ws? (car cs)))
             (define txt (token-text (cst-ws-tok (car cs))))
             (if (string-contains? txt "\n")
                 (cdr cs)
                 (loop (cdr cs) #t))]
            [else cs])))
      (append before trimmed))))

(define (cst-wrap-form root path opener-str closer-str)
  (define opener-type
    (case opener-str [("(") 'open-paren] [("[") 'open-bracket] [("{") 'open-brace] [("#{") 'hash-open-brace]
      [else (error 'cst-wrap-form "unknown opener: ~a" opener-str)]))
  (define closer-type
    (case closer-str [(")") 'close-paren] [("]") 'close-bracket] [("}") 'close-brace]
      [else (error 'cst-wrap-form "unknown closer: ~a" closer-str)]))
  (cst-modify-at root path
    (lambda (children ai)
      (define target (list-ref children ai))
      (define wrapped
        (cst-list (token opener-type opener-str 0 0 0)
                  (list target)
                  (token closer-type closer-str 0 0 0)))
      (list-replace-at children ai wrapped))))

(define (cst-rename-symbol root old-name new-name)
  (define (rename node)
    (cond
      [(cst-root? node)
       (cst-root (map rename (cst-root-children node)))]
      [(cst-list? node)
       (cst-list (cst-list-opener node)
                 (map rename (cst-list-children node))
                 (cst-list-closer node))]
      [(cst-atom? node)
       (define tok (cst-atom-tok node))
       (if (equal? (token-text tok) old-name)
           (cst-atom (token (token-type tok) new-name
                            (token-line tok) (token-col tok) (token-offset tok)))
           node)]
      [else node]))
  (rename root))

(define (cst-add-binding root let-path name-text value-text)
  (define let-form (cst-find-path root let-path))
  (unless (and let-form (cst-list? let-form))
    (error 'cst-add-binding "path does not point to a list form"))
  (define cc (cst-content-children let-form))
  (unless (and (>= (length cc) 2)
               (cst-list? (cadr cc)))
    (error 'cst-add-binding "expected binding list as second content child"))

  ;; Find the binding vector (second content child of let form)
  (define binding-path (append let-path '(1)))
  (define binding-text (format "[~a ~a]" name-text value-text))
  (define binding-nodes (text->cst-nodes binding-text))

  (cst-modify-at root binding-path
    (lambda (children ai)
      (define bindings-form (list-ref children ai))
      (define inner (cst-list-children bindings-form))
      (define ws (cst-ws (token 'newline "\n" 0 0 0)))
      (define indent (cst-ws (token 'whitespace "        " 0 0 0)))
      (define new-inner (append inner (list ws indent) binding-nodes))
      (list-replace-at children ai
        (cst-list (cst-list-opener bindings-form)
                  new-inner
                  (cst-list-closer bindings-form))))))

(define (cst-add-map-entry root map-path key-text val-text)
  (define map-form (cst-find-path root map-path))
  (unless (and map-form (cst-list? map-form))
    (error 'cst-add-map-entry "path does not point to a list/map form"))

  (define entry-nodes (text->cst-nodes (format "~a ~a" key-text val-text)))

  (cst-modify-at root map-path
    (lambda (children ai)
      (define mf (list-ref children ai))
      (define inner (cst-list-children mf))
      (define ws (cst-ws (token 'newline "\n" 0 0 0)))
      (define indent (cst-ws (token 'whitespace "   " 0 0 0)))
      (define new-inner (append inner (list ws indent) entry-nodes))
      (list-replace-at children ai
        (cst-list (cst-list-opener mf) new-inner (cst-list-closer mf))))))
