#lang racket/base

;; Durable coverage for the beagle-repair clause-insertion consumer: runs the
;; pure-python unit tests in repair_apply_test.py (the apply logic is extracted
;; into bin/beagle_repair_apply.py precisely so it is testable without the full
;; repair pipeline). See:
;;   ~/code/life-os/threads/20260615005103-beagle_python_repair_consume_structured.md

(require rackunit
         racket/system
         racket/runtime-path)

(define-runtime-path here ".")

(define python
  (or (find-executable-path "python3")
      (find-executable-path "python")))

;; python3 is a hard dependency of bin/beagle-repair, so its absence is a real
;; failure, not a skip.
(test-case "python3 available (required by bin/beagle-repair)"
  (check-true (and python #t) "python3 not found — required by bin/beagle-repair"))

(test-case "beagle-repair insert_match_clauses python unit tests pass"
  (when python
    (define script (path->string (build-path here "repair_apply_test.py")))
    (check-true (system* python script)
                "repair_apply_test.py reported failures (see output above)")))
