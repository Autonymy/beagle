#lang racket/base

(require rackunit
         racket/port
         "../private/parse.rkt"
         "../private/lint.rkt")

(define (lint-prog . forms)
  (define prog (parse-program (map (lambda (f) (datum->syntax #f f)) forms)))
  (define out (open-output-string))
  (parameterize ([current-error-port out])
    (lint-program! prog))
  (get-output-string out))

(test-case "untyped def warns in strict mode"
  (define out (lint-prog '(def x 42)))
  (check-true (regexp-match? #rx"untyped def x" out)))

(test-case "typed def does not warn"
  (define out (lint-prog '(def x : Long 42)))
  (check-equal? out ""))

(test-case "defn without return type warns"
  (define out (lint-prog '(defn foo [(x : Long)] x)))
  (check-true (regexp-match? #rx"defn foo has no return type" out)))

(test-case "defn with untyped params warns"
  (define out (lint-prog '(defn foo [x y] : Long (+ x y))))
  (check-true (regexp-match? #rx"defn foo has untyped parameter" out)))

(test-case "fully typed defn produces no warnings"
  (define out (lint-prog '(defn foo [(x : Long) (y : Long)] : Long (+ x y))))
  (check-equal? out ""))

(test-case "unsafe inline warns"
  (define out (lint-prog '(unsafe "(println :hi)")))
  (check-true (regexp-match? #rx"unsafe.*inline escape" out)))

(test-case "lint skipped in dynamic mode"
  (define out (lint-prog '(define-mode dynamic)
                         '(def x 42)
                         '(defn foo [x] x)))
  (check-equal? out ""))
