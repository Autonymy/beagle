#lang racket/base

;; Compile-time evaluation of pure operatives — the macro-as-operative
;; win per plan 20260528223000.
;;
;; The key claim of the operative foundation: macros are not a separate
;; language feature. They are operatives the compiler chooses to evaluate
;; at compile time because they are pure (no mutation in their dynamic
;; extent). Beagle's explicit-mutation discipline is what makes this
;; safe — pure operatives stay pure, so the compiler can evaluate them
;; ahead of time and trust the result.
;;
;; Implementation: an expand pass that walks the program, evaluating
;; calls to known macros (operatives registered via define-macro). The
;; macro receives its raw arguments (operative semantics) and returns
;; data which is spliced into the program in place of the original call.
;;
;; This is the same "macros are functions from syntax to syntax"
;; concept as traditional Lisp, but unified: a macro is an operative,
;; and an operative is a value. There is no phase separation.

(require racket/match
         "eval.rkt"
         "eval-standard.rkt")

(provide expand-program)

(define QUOTE-OP-SYM (string->symbol "'"))

;; --- macro registration -------------------------------------------------

;; A macro is a raw operative the compiler can evaluate at compile time.
;; We register them in a hash keyed by name; the value is the operative.
(define (make-macro-registry) (make-hasheq))

(define (register-macro! reg name op)
  (hash-set! reg name op))

(define (lookup-macro reg name)
  (hash-ref reg name #f))

;; --- the expand pass ----------------------------------------------------

(define (expand-program forms)
  ;; Returns a list of expanded forms. Walks each form; for define-macro
  ;; forms, registers the macro and emits nothing; for calls to known
  ;; macros, evaluates the macro and emits the result; otherwise,
  ;; recurses into sub-expressions.
  (define reg (make-macro-registry))
  ;; The compile-time env: a fresh operative env into which we install
  ;; the standard forms (so macros can use defn/let/if/etc. internally)
  ;; and into which define-macro registers the macro operative.
  (define env (initial-env))
  (install-standard-forms! env)
  (apply append
    (for/list ([f (in-list forms)])
      (expand-form f reg env))))

(define (expand-form form reg env)
  ;; Returns a list of expanded forms (may be 0+).
  (cond
    [(and (pair? form) (eq? (car form) 'define-macro))
     (register-define-macro! form reg env)
     '()]
    [(and (pair? form) (symbol? (car form)) (lookup-macro reg (car form)))
     ;; Macro call — expand it and recursively expand the result.
     (define op (lookup-macro reg (car form)))
     (define expansion-failed? #f)
     (define result
       (with-handlers ([exn:fail?
                        (lambda (e)
                          (set! expansion-failed? #t)
                          (cons (car form)
                                (apply append
                                  (for/list ([a (in-list (cdr form))])
                                    (expand-form a reg env)))))])
         (apply-operative op (cdr form) env)))
     (cond
       [expansion-failed?
        ;; Don't recurse on the original form — that would loop.
        (list result)]
       [(and (pair? result) (eq? (car result) '#%splice))
        (apply append
          (for/list ([sub (in-list (cdr result))])
            (expand-form sub reg env)))]
       [else
        (expand-form result reg env)])]
    [(pair? form)
     ;; Recurse into call args (preserving the operator)
     (list (cons (car form)
                 (apply append
                   (for/list ([a (in-list (cdr form))])
                     (expand-form a reg env)))))]
    [else (list form)]))

(define (register-define-macro! form reg env)
  ;; Surface shapes (mirroring the migration tool's emission):
  ;;   (define-macro safe   NAME (' params P...) template)
  ;;   (define-macro proc   NAME (' params P...) ∈ RT (body B...))
  ;;   (define-macro beagle NAME (' params P...) ∈ RT (body B...))
  ;;
  ;; All three become operatives:
  ;;   safe   → operative whose body returns the substituted template
  ;;   proc   → operative whose body runs (body B...) and returns the result
  ;;   beagle → same as proc but evaluated via the operative env (which is
  ;;            already where everything runs in this implementation)
  (match form
    [(list 'define-macro 'safe (? symbol? name) params-form template)
     (register-macro! reg name (build-safe-macro params-form template env))]
    [(list 'define-macro (and kind (or 'proc 'beagle)) (? symbol? name)
           params-form '∈ _ret-type body-form)
     (register-macro! reg name (build-proc-macro params-form body-form env))]
    [(list 'define-macro (and kind (or 'proc 'beagle)) (? symbol? name)
           params-form body-form)
     (register-macro! reg name (build-proc-macro params-form body-form env))]
    [_ (void)]))

(define (build-safe-macro params-form template env)
  ;; A safe (template) macro: receives args raw, substitutes into the
  ;; template (replacing param references), and returns the result.
  (define params (extract-names params-form env))
  (operative-from name-of-macro
    (lambda (args env)
      (substitute-template template (map cons params args)))))

(define (build-proc-macro params-form body-form env)
  ;; A proc/beagle macro: receives args raw, binds them to params in a
  ;; fresh env, evaluates the body, returns the result.
  (define params (extract-names params-form env))
  (operative-from 'macro-proc
    (lambda (args call-env)
      (define new-env (env-extend env))
      (let bind ([ps params] [as args])
        (cond
          [(null? ps) (void)]
          [(null? as) (error 'macro "too few args, missing ~a" (car ps))]
          [else
           (env-define! new-env (car ps) (car as))
           (bind (cdr ps) (cdr as))]))
      (evaluate body-form new-env))))

(define (operative-from name proc)
  ;; Use the exported make-raw-operative from eval.rkt.
  (make-raw-operative name proc))

(define name-of-macro 'macro-safe)

(define (extract-names form env)
  ;; form: (' params NAME...). Evaluate ' to get (params NAME...), then drop head.
  (define data
    (with-handlers ([exn:fail? (lambda (_) form)])
      (evaluate form env)))
  (cond
    [(and (pair? data) (memq (car data) '(params fields vars)))
     (cdr data)]
    [(pair? data) data]
    [else '()]))

(define (substitute-template template subs)
  ;; Replace any param-name occurrences in template with the substituted args.
  (cond
    [(symbol? template)
     (define entry (assq template subs))
     (if entry (cdr entry) template)]
    [(pair? template)
     (cons (substitute-template (car template) subs)
           (substitute-template (cdr template) subs))]
    [else template]))
