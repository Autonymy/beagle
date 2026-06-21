#lang racket/base

;; G-A — cross-module `binding` of a required module's `^:dynamic` var.
;;
;; Clojure binds any other-namespace dynamic var (`(binding [other/*x* v] ...)`
;; is standard); beagle used to REJECT it ("a/*v* is not a dynamic var") because
;; the checker only consulted the CURRENT module's dynamic-var set, never the
;; required module's. The importer now carries each imported `^:dynamic` var's
;; dynamic-ness (keyed by the use-site name), so `binding` resolves it across the
;; module boundary — matching Clojure. This blocked fram's resolver-woven daemon
;; port (handlers bind resolve/* dynvars cross-module). Logged to
;; hallucinations.jsonl as a surface-coherence divergence.
;;
;; The fix must stay PRECISE: binding a non-dynamic imported var is still an error.

(require rackunit
         racket/runtime-path
         beagle/private/parse
         beagle/private/check)

(define-runtime-path fixtures-dir "fixtures/dynvar-xmodule")

(define (check-file name)
  (define src (build-path fixtures-dir name))
  (type-check! (parse-program (read-beagle-syntax src) #:source-path src)))

(test-case "a requiring module can `binding` an imported ^:dynamic var (G-A, matches Clojure)"
  (check-not-exn (lambda () (check-file "ok.bclj"))))

(test-case "binding a NON-dynamic imported var is still rejected (fix stays precise)"
  (check-exn #rx"not a dynamic var"
             (lambda () (check-file "bad.bclj"))))
