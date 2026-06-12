#lang racket/base
;; Compile src/sim_kernel.bgl through BOTH backends:
;;   zig -> src/sim.zig          (the kernel the harness runs)
;;   clj -> bb/sim_kernel.clj    (the babashka differential oracle)
;; Same source, two emissions — the heart of the conformance story.

(require beagle/private/parse
         beagle/private/check
         beagle/private/emit)

(define here (path-only (path->complete-path (find-system-path 'run-file))))
(define src (build-path here "src" "sim_kernel.bgl"))

(define (compile-to target out)
  (define stxs (read-beagle-syntax src))
  (define forms (cons (datum->syntax #f (list 'define-target target)) stxs))
  (define prog (parse-program forms #:source-path src))
  (type-check! prog)
  (call-with-output-file out #:exists 'replace
    (lambda (p) (display (emit-program prog) p)))
  (printf "~a -> ~a\n" target out))

(require racket/path)
(compile-to 'zig (build-path here "src" "sim.zig"))
(compile-to 'clj (build-path here "bb" "sim_kernel.clj"))
