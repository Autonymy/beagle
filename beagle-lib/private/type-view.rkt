#lang racket/base

;; types-as-view: project the checker's knowledge back into beagle surface.
;;
;; The clean view is literally your source (proving the anti-reification point
;; — nothing is stored). The `inferred` and `all` views take that same source
;; and TEXT-INJECT `:- T` / `^T` annotations at precise positions recovered
;; from the src-table + the per-node inferred types captured during checking
;; (ast.rkt current-type-table). No type lives in the source; the view is a
;; pure function of the checked program. This is beagle's delaborator — the
;; inverse of elaboration (cf. Lean's PrettyPrinter/Delaborator).
;;
;; CLI:  beagle-explain-type FILE [NAME] [--level clean|inferred|all]

(require racket/list
         racket/string
         racket/file
         "parse.rkt"
         "check.rkt"
         "types.rkt"
         "ast.rkt")

(provide explain-type)

;; --- generic AST walk (transparent structs) ---------------------------------

;; Field list of a transparent struct instance, or #f for non-structs.
(define (struct-fields x)
  (with-handlers ([exn:fail? (lambda _ #f)])
    (define v (struct->vector x))
    (and (> (vector-length v) 0)
         (symbol? (vector-ref v 0))
         (regexp-match? #rx"^struct:" (symbol->string (vector-ref v 0)))
         (cdr (vector->list v)))))

;; All nodes in x (and its substructure) satisfying pred, in document order.
;; A visited set guards against cycles AND shared substructure (e.g.
;; threading-marker holds both orig-args and desugared, which share nodes —
;; naive recursion would be exponential).
(define (deep-collect pred x)
  (define out '())
  (define seen (make-hasheq))
  (let go ([x x])
    (cond
      [(or (string? x) (symbol? x) (number? x) (boolean? x) (null? x)) (void)]
      [(and (or (pair? x) (struct-fields x)) (hash-ref seen x #f)) (void)]
      [else
       (when (or (pair? x) (struct-fields x)) (hash-set! seen x #t))
       (when (pred x) (set! out (cons x out)))
       (cond
         [(pair? x) (go (car x)) (go (cdr x))]
         [(struct-fields x) => (lambda (fs) (for-each go fs))]
         [else (void)])]))
  (reverse out))

;; --- offsets ----------------------------------------------------------------

;; Vector v where v[k-1] = char offset of the start of (1-based) line k.
(define (line-offsets text)
  (define offs (list 0))
  (for ([ch (in-string text)] [i (in-naturals)])
    (when (char=? ch #\newline) (set! offs (cons (add1 i) offs))))
  (list->vector (reverse offs)))

(define (loc->offset offs loc)
  (and (src-loc? loc)
       (src-loc-line loc) (src-loc-col loc)
       (let ([li (sub1 (src-loc-line loc))])
         (and (>= li 0) (< li (vector-length offs))
              (+ (vector-ref offs li) (src-loc-col loc))))))

;; Apply (offset . insert-string) edits to text, right-to-left so earlier
;; offsets stay valid.
(define (apply-edits text edits)
  (for/fold ([t text]) ([e (in-list (sort edits > #:key car))])
    (string-append (substring t 0 (car e)) (cdr e) (substring t (car e)))))

;; --- form lookup ------------------------------------------------------------

(define (form-name f)
  (cond [(defn-form? f)  (defn-form-name f)]
        [(def-form? f)   (def-form-name f)]
        [(defonce-form? f) (defonce-form-name f)]
        [(defn-multi? f) (defn-multi-name f)]
        [else #f]))

;; Returns (values form form-stx) for NAME, or (values #f #f).
(define (find-form prog name)
  (let loop ([fs (program-forms prog)] [ss (program-form-stxs prog)])
    (cond
      [(or (null? fs) (null? ss)) (values #f #f)]
      [(equal? (form-name (car fs)) name) (values (car fs) (car ss))]
      [else (loop (cdr fs) (cdr ss))])))

;; --- view construction ------------------------------------------------------

;; The source substring of one top-level form, from its syntax position/span.
(define (form-source text form-stx)
  (define pos (syntax-position form-stx))   ; 1-based char offset
  (define span (syntax-span form-stx))
  (if (and pos span)
      (substring text (sub1 pos) (min (string-length text) (+ (sub1 pos) span)))
      text))

;; inferred: inject `:- T` before each un-annotated let-binding value whose
;; type was captured. Offsets are relative to the form substring.
(define (annotate-inferred text form form-stx src-tbl ty-tbl)
  (define base (sub1 (or (syntax-position form-stx) 1)))
  (define offs (line-offsets text))
  (define edits
    (for*/list ([b (in-list (deep-collect let-binding? form))]
                #:when (not (let-binding-type b))            ; skip already-annotated
                [v (in-value (let-binding-value b))]
                [loc (in-value (hash-ref src-tbl v #f))]
                [ty (in-value (hash-ref ty-tbl v #f))]
                [abs (in-value (and loc (loc->offset offs loc)))]
                #:when (and abs ty))
      (cons (- abs base) (string-append ":- " (type->string ty) " "))))
  (apply-edits (form-source text form-stx) edits))

;; all: pp.all — prefix every typed+positioned node inside the form with ^T.
(define (annotate-all text form form-stx src-tbl ty-tbl)
  (define start (sub1 (or (syntax-position form-stx) 1)))
  (define end (+ start (or (syntax-span form-stx) (string-length text))))
  (define offs (line-offsets text))
  (define edits
    (for*/list ([(node ty) (in-hash ty-tbl)]
                [loc (in-value (hash-ref src-tbl node #f))]
                [abs (in-value (and loc (loc->offset offs loc)))]
                #:when (and abs (>= abs start) (< abs end)))
      (cons (- abs start) (string-append "^" (type->string ty) " "))))
  (apply-edits (form-source text form-stx) edits))

;; --- entry ------------------------------------------------------------------

;; Returns a string (the rendered view) or raises a user error.
(define (explain-type path #:name [name #f] #:level [level "clean"])
  (define text (file->string path))
  (define stxs (read-beagle-syntax path))
  (define prog (parse-program stxs #:source-path path))
  ;; check to populate the per-node type table (errors are tolerated — a
  ;; file with a type error still yields partial inferred types).
  (type-check-with-locs! prog (lambda (e stx) (void)))
  (define src-tbl (or (program-src-table prog) (make-hasheq)))
  (define ty-tbl  (or (program-type-table prog) (make-hasheq)))
  (define-values (form form-stx)
    (if name (find-form prog (string->symbol name)) (values #f #f)))
  (when (and name (not form))
    (error 'beagle-explain-type "no top-level definition named `~a` in ~a" name path))
  (cond
    [(not form) text]   ; no NAME: clean view of the whole file
    [(string=? level "clean")    (form-source text form-stx)]
    [(string=? level "inferred") (annotate-inferred text form form-stx src-tbl ty-tbl)]
    [(string=? level "all")      (annotate-all text form form-stx src-tbl ty-tbl)]
    [else (error 'beagle-explain-type "unknown --level ~a (use clean|inferred|all)" level)]))

;; --- CLI --------------------------------------------------------------------

(module+ main
  (define args (vector->list (current-command-line-arguments)))
  ;; manual parse so --level works in any position
  (define level
    (let loop ([a args])
      (cond [(null? a) "clean"]
            [(and (string=? (car a) "--level") (pair? (cdr a))) (cadr a)]
            [else (loop (cdr a))])))
  (define positional
    (let strip ([a args])
      (cond [(null? a) '()]
            [(string=? (car a) "--level") (strip (cddr a))]
            [else (cons (car a) (strip (cdr a)))])))
  (cond
    [(null? positional)
     (eprintf "usage: beagle-explain-type FILE [NAME] [--level clean|inferred|all]\n")
     (exit 2)]
    [else
     (define file (car positional))
     (define name (and (pair? (cdr positional)) (cadr positional)))
     (with-handlers ([exn:fail? (lambda (e) (eprintf "~a\n" (exn-message e)) (exit 1))])
       (display (explain-type file #:name name #:level level))
       (newline))]))
