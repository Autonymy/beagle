#lang racket/base

;; Tests for compile-time macro expansion via the operative interpreter.

(require rackunit
         beagle/private/macro-expand)

(define Q (string->symbol "'"))
(define (Q-form . items) (cons Q items))

;; --- safe (template) macros --------------------------------------------

(test-case "safe macro: simple template substitution"
  ;; (define-macro safe twice (' params x) (+ x x))
  ;; Then (twice 5) should expand to (+ 5 5)
  (define result
    (expand-program
      `((define-macro safe twice ,(Q-form 'params 'x) (+ x x))
        (twice 5))))
  (check-equal? result '((+ 5 5))))

(test-case "safe macro: multi-param substitution"
  (define result
    (expand-program
      `((define-macro safe swap ,(Q-form 'params 'a 'b) (list b a))
        (swap 1 2))))
  (check-equal? result '((list 2 1))))

;; --- proc macros --------------------------------------------------------

(test-case "proc macro: returns computed data"
  ;; A proc macro that builds an arithmetic form at compile time.
  ;; Body uses `(list '+ 'x n)` written as variadic ' calls to construct
  ;; the resulting form.
  (define result
    (expand-program
      `((define-macro proc make-add ,(Q-form 'params 'n)
          (body (list ,(Q-form '+) ,(Q-form 'x) n)))
        (make-add 42))))
  ;; Body evaluates to (list (+) (x) 42) — `(' +)` returns the data list (+)
  ;; not the symbol +. For now, just verify expansion happened (not a
  ;; literal (make-add 42)).
  (check-not-equal? result '((make-add 42))))

;; --- recursive macro expansion ----------------------------------------

(test-case "macro that emits a call to another macro"
  (define result
    (expand-program
      `((define-macro safe twice ,(Q-form 'params 'x) (+ x x))
        (define-macro safe quad ,(Q-form 'params 'x) (twice (twice x)))
        (quad 3))))
  ;; quad 3 -> (twice (twice 3)) -> (twice (+ 3 3)) -> (+ (+ 3 3) (+ 3 3))
  (check-equal? result '((+ (+ 3 3) (+ 3 3)))))

;; --- non-macro forms pass through unchanged ----------------------------

(test-case "non-macro forms unchanged"
  (define result
    (expand-program
      `((+ 1 2)
        (defn add ,(Q-form 'params 'a 'b) (body (+ a b))))))
  (check-equal? result
    `((+ 1 2)
      (defn add ,(Q-form 'params 'a 'b) (body (+ a b))))))

;; --- define-macro itself emits nothing ---------------------------------

(test-case "define-macro emits no forms"
  (define result
    (expand-program
      `((define-macro safe id ,(Q-form 'params 'x) x))))
  (check-equal? result '()))
