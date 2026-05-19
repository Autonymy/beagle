#lang racket/base

(require racket/list
         "tokenize.rkt")

(provide
 (struct-out cst-root)
 (struct-out cst-list)
 (struct-out cst-atom)
 (struct-out cst-ws)
 (struct-out cst-comment)

 build-cst
 cst->string
 cst-content-children
 cst-find-path
 cst-find-by-line
 cst-find-defn

 ;; For patch module
 container-children
 update-container
 content-idx->actual-idx)

(struct cst-root (children) #:transparent)
(struct cst-list (opener children closer) #:transparent)
(struct cst-atom (tok) #:transparent)
(struct cst-ws (tok) #:transparent)
(struct cst-comment (tok) #:transparent)

;; ---------------------------------------------------------------------------
;; Build CST from tokens
;; ---------------------------------------------------------------------------

(define (build-cst tokens)
  (cst-root (parse-top-level tokens)))

(define (parse-top-level tokens)
  (let loop ([toks tokens] [acc '()])
    (cond
      [(null? toks) (reverse acc)]
      [else
       (define tok (car toks))
       (define ty (token-type tok))
       (cond
         [(opener? tok)
          (define-values (children rest) (parse-children (cdr toks) ty))
          (define close-tok
            (and (pair? rest)
                 (closer? (car rest))
                 (memq ty (openers-for-closer (token-type (car rest))))
                 (car rest)))
          (loop (if close-tok (cdr rest) rest)
                (cons (cst-list tok children close-tok) acc))]
         [(closer? tok)
          (loop (cdr toks) (cons (cst-atom tok) acc))]
         [(memq ty '(whitespace newline))
          (loop (cdr toks) (cons (cst-ws tok) acc))]
         [(memq ty '(line-comment block-comment))
          (loop (cdr toks) (cons (cst-comment tok) acc))]
         [else
          (loop (cdr toks) (cons (cst-atom tok) acc))])])))

(define (parse-children tokens stop-opener-type)
  (let loop ([toks tokens] [acc '()])
    (cond
      [(null? toks)
       (values (reverse acc) '())]
      [else
       (define tok (car toks))
       (define ty (token-type tok))
       (cond
         [(and (closer? tok)
               (memq stop-opener-type (openers-for-closer ty)))
          (values (reverse acc) toks)]
         [(closer? tok)
          (values (reverse acc) toks)]
         [(opener? tok)
          (define-values (children rest) (parse-children (cdr toks) ty))
          (define close-tok
            (and (pair? rest)
                 (closer? (car rest))
                 (memq ty (openers-for-closer (token-type (car rest))))
                 (car rest)))
          (loop (if close-tok (cdr rest) rest)
                (cons (cst-list tok children close-tok) acc))]
         [(memq ty '(whitespace newline))
          (loop (cdr toks) (cons (cst-ws tok) acc))]
         [(memq ty '(line-comment block-comment))
          (loop (cdr toks) (cons (cst-comment tok) acc))]
         [else
          (loop (cdr toks) (cons (cst-atom tok) acc))])])))

;; ---------------------------------------------------------------------------
;; Serialize CST to text (identity roundtrip for balanced input)
;; ---------------------------------------------------------------------------

(define (cst->string node)
  (define out (open-output-string))
  (cst-write node out)
  (get-output-string out))

(define (cst-write node port)
  (cond
    [(cst-root? node)
     (for-each (lambda (c) (cst-write c port)) (cst-root-children node))]
    [(cst-list? node)
     (display (token-text (cst-list-opener node)) port)
     (for-each (lambda (c) (cst-write c port)) (cst-list-children node))
     (when (cst-list-closer node)
       (display (token-text (cst-list-closer node)) port))]
    [(cst-atom? node) (display (token-text (cst-atom-tok node)) port)]
    [(cst-ws? node) (display (token-text (cst-ws-tok node)) port)]
    [(cst-comment? node) (display (token-text (cst-comment-tok node)) port)]))

;; ---------------------------------------------------------------------------
;; Navigation
;; ---------------------------------------------------------------------------

(define (container-children node)
  (cond
    [(cst-root? node) (cst-root-children node)]
    [(cst-list? node) (cst-list-children node)]
    [else '()]))

(define (update-container node new-children)
  (cond
    [(cst-root? node) (cst-root new-children)]
    [(cst-list? node)
     (cst-list (cst-list-opener node) new-children (cst-list-closer node))]
    [else node]))

(define (cst-content-children node)
  (filter (lambda (c) (or (cst-list? c) (cst-atom? c)))
          (container-children node)))

(define (content-idx->actual-idx children content-idx)
  (let loop ([cs children] [ci 0] [ai 0])
    (cond
      [(null? cs) #f]
      [(or (cst-ws? (car cs)) (cst-comment? (car cs)))
       (loop (cdr cs) ci (add1 ai))]
      [(= ci content-idx) ai]
      [else (loop (cdr cs) (add1 ci) (add1 ai))])))

(define (cst-find-path node path)
  (cond
    [(null? path) node]
    [else
     (define cc (cst-content-children node))
     (define idx (car path))
     (and (< idx (length cc))
          (cst-find-path (list-ref cc idx) (cdr path)))]))

(define (cst-find-by-line node target-line)
  (define (node-start n)
    (cond
      [(cst-list? n) (token-line (cst-list-opener n))]
      [(cst-atom? n) (token-line (cst-atom-tok n))]
      [else #f]))
  (define (node-end n)
    (cond
      [(cst-list? n) (and (cst-list-closer n) (token-line (cst-list-closer n)))]
      [(cst-atom? n) (token-line (cst-atom-tok n))]
      [else #f]))
  (define (search n)
    (cond
      [(cst-root? n)
       (for/or ([c (in-list (cst-root-children n))]) (search c))]
      [(and (cst-list? n)
            (let ([s (node-start n)] [e (node-end n)])
              (and s e (<= s target-line) (>= e target-line))))
       (or (for/or ([c (in-list (cst-list-children n))]) (search c))
           n)]
      [else #f]))
  (search node))

(define (cst-find-defn node name)
  (define name-str (if (symbol? name) (symbol->string name) name))
  (for/or ([form (in-list (cst-content-children node))])
    (and (cst-list? form)
         (let ([cc (cst-content-children form)])
           (and (>= (length cc) 2)
                (cst-atom? (car cc))
                (member (token-text (cst-atom-tok (car cc))) '("define" "defn" "def"))
                (let ([second (cadr cc)])
                  (cond
                    [(and (cst-atom? second)
                          (equal? (token-text (cst-atom-tok second)) name-str))
                     form]
                    [(and (cst-list? second)
                          (pair? (cst-content-children second))
                          (cst-atom? (car (cst-content-children second)))
                          (equal? (token-text (cst-atom-tok (car (cst-content-children second))))
                                  name-str))
                     form]
                    [else #f])))))))
