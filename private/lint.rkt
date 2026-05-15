#lang racket/base

;; Lint warnings — best-practice flags that don't fail compile.
;;
;; Print to stderr so they're visible during build but don't pollute the
;; stdout that bin/beagle-build pipes to .clj files.
;;
;; Skipped in dynamic mode (types are optional there by definition).

(require racket/match
         racket/format
         "parse.rkt"
         "types.rkt")

(define (lint-program! prog)
  (when (eq? (program-mode prog) 'strict)
    (for ([form (in-list (program-forms prog))])
      (lint-form form))))

(define (lint-form f)
  (cond
    [(def-form? f) (lint-def f)]
    [(defn-form? f) (lint-defn f)]
    [(unsafe-clj? f) (lint-unsafe-clj f)]
    [else (void)]))

(define (warn fmt . args)
  (apply fprintf (current-error-port)
         (string-append "beagle [lint]: " fmt "\n")
         args))

(define (lint-def f)
  (unless (def-form-type f)
    (warn "untyped def ~a (consider adding `: Type`)"
          (def-form-name f))))

(define (lint-defn f)
  (define name (defn-form-name f))
  (define params (defn-form-params f))
  (define ret (defn-form-return-type f))
  (unless ret
    (warn "defn ~a has no return type annotation (consider adding `: ReturnType`)"
          name))
  (define untyped-params
    (for/list ([p (in-list params)]
               #:unless (param-type p))
      (param-name p)))
  (unless (null? untyped-params)
    (warn "defn ~a has untyped parameter(s): ~a (consider adding `(name : Type)`)"
          name
          (string-join (map symbol->string untyped-params) ", "))))

(define (lint-unsafe-clj _)
  (warn "(unsafe \"...\") inline escape — beagle cannot type-check this code"))

(define (string-join xs sep)
  (cond
    [(null? xs) ""]
    [(null? (cdr xs)) (car xs)]
    [else (string-append (car xs) sep (string-join (cdr xs) sep))]))

(provide lint-program!)
