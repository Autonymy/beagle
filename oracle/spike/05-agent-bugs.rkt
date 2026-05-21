#lang typed/racket

;; Spike 5: Two agent bugs from E16 that Beagle's type checker catches
;;
;; Bug A (type confusion — id swap):
;;   Assignment takes (task-id, worker-id). An agent swaps the arguments,
;;   passing worker-id where task-id is expected. Without newtypes both are
;;   String so the swap is invisible. With newtypes (defscalar TaskId String)
;;   Typed Racket rejects the mismatch.
;;
;; Bug B (non-exhaustive match):
;;   ScheduleAttempt is (U AttemptSuccess AttemptFailure). An agent removes
;;   the AttemptFailure arm. Typed Racket's occurrence typing warns about
;;   uncovered variants.

;; ─── Shared types ───

(struct Window ([start : Integer] [end : Integer]) #:transparent)
(struct RetryPolicy ([max-attempts : Integer] [backoff : Integer]) #:transparent)

(struct Task ([id : String] [name : String] [duration : Integer]
              [priority : Integer] [deadline : Integer]) #:transparent)

(struct Worker ([id : String] [name : String]
               [capabilities : (Listof String)]) #:transparent)

;; ═══════════════════════════════════════════════════════════════
;; Bug A: id swap caught by newtypes
;; ═══════════════════════════════════════════════════════════════

;; Newtypes: distinct wrapper types around String.
;; Typed Racket doesn't have Haskell-style newtypes, but we can
;; model them as single-field structs — the same strategy Beagle's
;; defscalar would emit.

(struct TaskId ([v : String]) #:transparent)
(struct WorkerId ([v : String]) #:transparent)

(struct Assignment ([task-id : TaskId]
                    [worker-id : WorkerId]
                    [start-time : Integer]
                    [end-time : Integer]) #:transparent)

;; ── correct version ──
(: make-assignment-correct (-> Task Worker Integer Integer Assignment))
(define (make-assignment-correct task worker start end)
  (Assignment (TaskId (Task-id task))
              (WorkerId (Worker-id worker))
              start end))

;; ── buggy version: swapped id arguments ──
;; This is the exact bug from E16 bug 04: the agent swaps task-id
;; and worker-id in the constructor call.
;; Uncomment to see Typed Racket reject it:
;;
;; (: make-assignment-buggy (-> Task Worker Integer Integer Assignment))
;; (define (make-assignment-buggy task worker start end)
;;   (Assignment (WorkerId (Worker-id worker))   ; ← TaskId slot gets WorkerId
;;               (TaskId (Task-id task))          ; ← WorkerId slot gets TaskId
;;               start end))
;;
;; Error: expected TaskId, given WorkerId

;; ═══════════════════════════════════════════════════════════════
;; Bug B: non-exhaustive match
;; ═══════════════════════════════════════════════════════════════

(struct ScheduleFailure ([task-id : String] [reason : String]) #:transparent)
(struct AttemptSuccess ([assignment : Assignment]) #:transparent)
(struct AttemptFailure ([failure : ScheduleFailure]) #:transparent)
(define-type ScheduleAttempt (U AttemptSuccess AttemptFailure))

(struct AccState ([assignments : (Listof Assignment)]
                  [failures : (Listof ScheduleFailure)]) #:transparent)

;; ── correct version: handles both arms ──
(: process-attempt (-> AccState ScheduleAttempt AccState))
(define (process-attempt acc attempt)
  (cond
    [(AttemptSuccess? attempt)
     (AccState (cons (AttemptSuccess-assignment attempt)
                     (AccState-assignments acc))
               (AccState-failures acc))]
    [(AttemptFailure? attempt)
     (AccState (AccState-assignments acc)
               (cons (AttemptFailure-failure attempt)
                     (AccState-failures acc)))]))

;; ── buggy version: missing AttemptFailure arm ──
;; This is the exact bug from E16 bug 06: the agent removes the
;; AttemptFailure pattern, making the match non-exhaustive.
;; Uncomment to see Typed Racket reject it:
;;
;; (: process-attempt-buggy (-> AccState ScheduleAttempt AccState))
;; (define (process-attempt-buggy acc attempt)
;;   (cond
;;     [(AttemptSuccess? attempt)
;;      (AccState (cons (AttemptSuccess-assignment attempt)
;;                      (AccState-assignments acc))
;;               (AccState-failures acc))]))
;;                               ;; ← AttemptFailure arm removed
;;                               ;; Typed Racket: function body has type
;;                               ;; (U AccState Void) — not AccState

;; ─── exercise it ───

(define t1 (Task "task-1" "Oil Change" 60 1 1440))
(define w1 (Worker "worker-1" "Alice" '("mechanical")))

(define a1 (make-assignment-correct t1 w1 100 160))
(ann a1 Assignment)

(displayln (Assignment-task-id a1))     ; (TaskId "task-1")
(displayln (Assignment-worker-id a1))   ; (WorkerId "worker-1")

(define empty-acc (AccState '() '()))
(define success (AttemptSuccess a1))
(define failure (AttemptFailure (ScheduleFailure "task-2" "no capable worker")))

(define acc1 (process-attempt empty-acc success))
(define acc2 (process-attempt acc1 failure))

(displayln (length (AccState-assignments acc2)))  ; 1
(displayln (length (AccState-failures acc2)))     ; 1
