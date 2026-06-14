#lang racket/base

;; types-as-view / the delaborator: beagle-explain-type projects the checker's
;; inferred per-node types back into surface, at three verbosity levels, with
;; NO type stored in the source (pure projection — the anti-reification point).

(require rackunit
         racket/file
         racket/string
         beagle/private/type-view)

(define SRC
  (string-append
   "#lang beagle/clj\n"
   "(defn process [n :- Int] :- Int\n"
   "  (let [a (* n 2)\n"
   "        b (+ a 1)]\n"
   "    (- b n)))\n"))

(define (with-fixture src thunk)
  (define tmp (make-temporary-file "type-view-~a.bclj"))
  (dynamic-wind
   (lambda () (call-with-output-file tmp
                (lambda (o) (display src o)) #:exists 'truncate/replace))
   (lambda () (thunk tmp))
   (lambda () (delete-file tmp))))

(test-case "clean view is the source as written (no interior types injected)"
  (with-fixture SRC
    (lambda (f)
      (define out (explain-type f #:name "process" #:level "clean"))
      ;; clean must be a byte-exact substring of the original source: no
      ;; annotations added (the file IS the clean view — anti-reification).
      (check-true (string-contains? SRC out)
                  "clean view must be verbatim source text")
      (check-true (string-contains? out "(* n 2)"))
      (check-false (string-contains? out ":- Int (* n 2)")
                   "clean must NOT inject inferred annotations"))))

(test-case "inferred view injects `:- T` on un-annotated let-bindings"
  (with-fixture SRC
    (lambda (f)
      (define out (explain-type f #:name "process" #:level "inferred"))
      (check-true (string-contains? out "a :- Int (* n 2)")
                  (format "expected inferred annotation on `a`; got:\n~a" out))
      (check-true (string-contains? out "b :- Int (+ a 1)")
                  (format "expected inferred annotation on `b`; got:\n~a" out))
      ;; the authored boundary annotation is preserved verbatim, not doubled
      (check-true (string-contains? out "[n :- Int]")))))

(test-case "all view prefixes every typed interior node with ^T"
  (with-fixture SRC
    (lambda (f)
      (define out (explain-type f #:name "process" #:level "all"))
      (check-true (string-contains? out "^Int")
                  (format "expected ^Int annotations; got:\n~a" out))
      ;; the binding values are typed Int
      (check-true (regexp-match? #rx"a \\^Int \\(\\* n 2\\)" out)))))

(test-case "no NAME yields the whole file (clean)"
  (with-fixture SRC
    (lambda (f)
      (define out (explain-type f))
      (check-true (string-contains? out "(defn process")))))

(test-case "unknown NAME is a pointed error"
  (with-fixture SRC
    (lambda (f)
      (check-exn #rx"no top-level definition named"
                 (lambda () (explain-type f #:name "nope" #:level "clean"))))))

(test-case "annotation-free source stays annotation-free in clean, gains types in inferred"
  ;; The headline: you author no interior types; the view summons them.
  (with-fixture SRC
    (lambda (f)
      (define clean (explain-type f #:name "process" #:level "clean"))
      (define inferred (explain-type f #:name "process" #:level "inferred"))
      (check-false (string-contains? clean ":- Int (* n 2)"))
      (check-true  (string-contains? inferred ":- Int (* n 2)"))
      ;; inferred is exactly clean + `:- Int ` injections: stripping them
      ;; recovers clean byte-for-byte (proves pure projection, no rewrite).
      (check-equal? (string-replace inferred ":- Int " "") clean))))
