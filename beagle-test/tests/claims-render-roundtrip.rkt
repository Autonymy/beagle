#lang racket/base

;; #17 regression: the claims renderer (`--render`) must reconstruct the `#lang`
;; header from the leading `(define-target X)` form (read-beagle-syntax
;; canonicalizes `#lang beagle/X` -> that form). A rendered module that led with
;; `(define-target …)` instead of `#lang` was rejected by bin/beagle check's
;; module loader ("expected a `module' declaration") — blocking fram's schema.bclj
;; flip-view (self-host 12/12). This drives source -> EDN -> render and asserts the
;; rendered output is a real #lang module AND re-reads to the identical forms.

(require rackunit
         rackunit/text-ui
         racket/port
         racket/file
         racket/string)

(define crt-path
  (path->string (collection-file-path "claims-roundtrip.rkt" "beagle" "private")))

(define (run . args)
  (define-values (proc out in err) (apply subprocess #f #f #f (find-executable-path "racket") crt-path args))
  (close-output-port in)
  (define o (port->string out))
  (define e (port->string err))
  (subprocess-wait proc)
  (close-input-port out) (close-input-port err)
  (values (subprocess-status proc) o e))

(define (render-roundtrip src-text)
  (define f (make-temporary-file "crt-~a.bclj"))
  (define edn (make-temporary-file "crt-~a.edn"))
  (dynamic-wind
    void
    (lambda ()
      (call-with-output-file f #:exists 'truncate (lambda (p) (display src-text p)))
      (define-values (c1 o1 e1) (run "--emit-edn" (path->string f)))
      (call-with-output-file edn #:exists 'truncate (lambda (p) (display o1 p)))
      (define-values (c2 o2 e2) (run "--render" (path->string edn)))
      o2)
    (lambda () (when (file-exists? f) (delete-file f)) (when (file-exists? edn) (delete-file edn)))))

(run-tests
 (test-suite "claims render — #lang reconstruction (#17)"

   (test-case "render reconstructs #lang beagle/clj from leading (define-target clj)"
     (define out (render-roundtrip "#lang beagle/clj\n\n;; hdr\n(def x :- Int 42)\n"))
     (check-true (string-prefix? out "#lang beagle/clj")
                 (format "rendered did not start with #lang:\n~a" out))
     (check-false (string-contains? out "(define-target")
                  "rendered still contains (define-target …)"))

   (test-case "render reconstructs #lang beagle/nix"
     (define out (render-roundtrip "#lang beagle/nix\n(def x :- Int 1)\n"))
     (check-true (string-prefix? out "#lang beagle/nix") out))))
