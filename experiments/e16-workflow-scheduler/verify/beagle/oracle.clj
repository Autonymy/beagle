#!/usr/bin/env bb
;; =============================================================================
;; E16 Workflow Scheduler — Verification Oracle (Beagle/Clojure)
;; =============================================================================
;;
;; Run with:
;;   cd /tmp/beagle-e16-out && bb -cp . <path>/oracle.clj
;;
;; Requires the golden Beagle scheduler compiled to Clojure at /tmp/beagle-e16-out/scheduler/

(require '[scheduler.scheduler :as s]
         '[scheduler.types :as t]
         '[scheduler.validator :as v])

;; =============================================================================
;; Test Infrastructure
;; =============================================================================

(def ^:dynamic *results* (atom []))

(defn run-test [name test-fn]
  (try
    (test-fn)
    (println "PASS:" name)
    (swap! *results* conj true)
    true
    (catch Exception e
      (println "FAIL:" name "-" (.getMessage e))
      (swap! *results* conj false)
      false)))

;; =============================================================================
;; Helpers — constructors
;; =============================================================================

(defn task
  "Create a task with all fields."
  [id name duration priority deadline caps resources deps retry-policy]
  (t/->Task id name duration priority deadline caps resources deps retry-policy))

(defn simple-task
  "Create a task with minimal fields (no deadline, caps, resources, deps, retry)."
  [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0)))

(defn task-with-deps
  "Create a task that depends on other tasks."
  [id name duration priority deps]
  (t/->Task id name duration priority 0 [] [] deps (t/->RetryPolicy 0 0)))

(defn task-with-caps
  "Create a task requiring capabilities."
  [id name duration priority caps]
  (t/->Task id name duration priority 0 caps [] [] (t/->RetryPolicy 0 0)))

(defn task-with-resources
  "Create a task requiring resources."
  [id name duration priority resources]
  (t/->Task id name duration priority 0 [] resources [] (t/->RetryPolicy 0 0)))

(defn task-with-deadline
  "Create a task with a deadline."
  [id name duration priority deadline]
  (t/->Task id name duration priority deadline [] [] [] (t/->RetryPolicy 0 0)))

(defn task-with-retry
  "Create a task with retry policy."
  [id name duration priority deadline caps retry-policy]
  (t/->Task id name duration priority deadline caps [] [] retry-policy))

(defn worker
  "Create a worker."
  [id name caps unavailable]
  (t/->Worker id name caps unavailable))

(defn simple-worker
  "Create a worker with no caps or unavailability."
  [id name]
  (t/->Worker id name [] []))

(defn worker-with-caps
  "Create a worker with capabilities."
  [id name caps]
  (t/->Worker id name caps []))

(defn worker-with-windows
  "Create a worker with unavailable windows."
  [id name windows]
  (t/->Worker id name [] windows))

(defn resource
  "Create a resource."
  [id name capacity]
  (t/->Resource id name capacity))

(defn window
  "Create a time window."
  [start end]
  (t/->Window start end))

(defn retry-policy [max-attempts backoff]
  (t/->RetryPolicy max-attempts backoff))

;; =============================================================================
;; Helpers — result inspection
;; =============================================================================

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn err? [result]
  (instance? scheduler.types.ScheduleError result))

(defn assignments [result]
  (:assignments result))

(defn failures [result]
  (:failures result))

(defn assignment-at [result idx]
  (nth (assignments result) idx))

(defn failure-at [result idx]
  (nth (failures result) idx))

(defn reason-type [failure]
  (type (:reason failure)))

(defn reason [failure]
  (:reason failure))

;; =============================================================================
;; 1. Happy Path Tests (~80 assertions)
;; =============================================================================

(defn happy-path-tests []
  (println "\n--- Happy Path ---")

  (run-test "empty input returns ScheduleOk with empty assignments"
    (fn []
      (let [r (s/schedule [] [] [])]
        (assert (ok? r) "expected ScheduleOk")
        (assert (empty? (assignments r)) "expected empty assignments"))))

  (run-test "empty tasks with workers returns ScheduleOk"
    (fn []
      (let [r (s/schedule [] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (empty? (assignments r))))))

  (run-test "empty tasks with workers and resources returns ScheduleOk"
    (fn []
      (let [r (s/schedule [] [(simple-worker "w1" "W1")] [(resource "r1" "R1" 1)])]
        (assert (ok? r))
        (assert (empty? (assignments r))))))

  (run-test "single task single worker"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 30 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 1 (count (assignments r))))
        (let [a (assignment-at r 0)]
          (assert (= "t1" (:task-id a)))
          (assert (= "w1" (:worker-id a)))
          (assert (= 0 (:start-time a)))
          (assert (= 30 (:end-time a)))
          (assert (= 1 (:attempt a)))))))

  (run-test "single task assigned at time 0"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (= 0 (:start-time (assignment-at r 0)))))))

  (run-test "two independent tasks with two workers run in parallel"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        (assert (= 2 (count (assignments r))))
        (let [a1 (assignment-at r 0)
              a2 (assignment-at r 1)]
          (assert (= "t1" (:task-id a1)))
          (assert (= "t2" (:task-id a2)))
          (assert (= "w1" (:worker-id a1)))
          (assert (= "w2" (:worker-id a2)))
          (assert (= 0 (:start-time a1)))
          (assert (= 0 (:start-time a2)))
          (assert (= 10 (:end-time a1)))
          (assert (= 10 (:end-time a2)))))))

  (run-test "two independent tasks one worker run sequentially"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 2 (count (assignments r))))
        (let [a1 (assignment-at r 0)
              a2 (assignment-at r 1)]
          (assert (= 0 (:start-time a1)))
          (assert (= 10 (:end-time a1)))
          (assert (= 10 (:start-time a2)))
          (assert (= 20 (:end-time a2)))))))

  (run-test "three independent tasks one worker sequential"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)
                           (simple-task "t3" "T3" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 3 (count (assignments r))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2)))))))

  (run-test "dependency chain A->B->C sequential"
    (fn []
      (let [ta (simple-task "t1" "T1" 10 1)
            tb (task-with-deps "t2" "T2" 10 1 ["t1"])
            tc (task-with-deps "t3" "T3" 10 1 ["t2"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 3 (count (assignments r))))
        (let [a1 (assignment-at r 0)
              a2 (assignment-at r 1)
              a3 (assignment-at r 2)]
          (assert (= "t1" (:task-id a1)))
          (assert (= "t2" (:task-id a2)))
          (assert (= "t3" (:task-id a3)))
          (assert (= 0 (:start-time a1)))
          (assert (= 10 (:start-time a2)))
          (assert (= 20 (:start-time a3)))
          (assert (= 10 (:end-time a1)))
          (assert (= 20 (:end-time a2)))
          (assert (= 30 (:end-time a3)))))))

  (run-test "priority ordering: lower priority number scheduled first"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 3)
                           (simple-task "t2" "T2" 10 1)
                           (simple-task "t3" "T3" 10 2)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        ;; priority 1 first, then 2, then 3
        (assert (= "t2" (:task-id (assignment-at r 0))))
        (assert (= "t3" (:task-id (assignment-at r 1))))
        (assert (= "t1" (:task-id (assignment-at r 2))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2)))))))

  (run-test "same priority tiebreak by id ASC"
    (fn []
      (let [r (s/schedule [(simple-task "t3" "T3" 10 1)
                           (simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= "t1" (:task-id (assignment-at r 0))))
        (assert (= "t2" (:task-id (assignment-at r 1))))
        (assert (= "t3" (:task-id (assignment-at r 2)))))))

  (run-test "deterministic worker selection: first by id ASC"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)]
                          [(simple-worker "w2" "W2")
                           (simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "multiple tasks deterministic worker selection"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)]
                          [(simple-worker "w3" "W3")
                           (simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0))))
        (assert (= "w2" (:worker-id (assignment-at r 1)))))))

  (run-test "assignment sort order: start_time ASC then task_id ASC"
    (fn []
      (let [;; t2 has higher priority (1), t1 has lower (2), so both start at 0 with 2 workers
            ;; Output sorted by (start_time ASC, task_id ASC)
            r (s/schedule [(simple-task "t2" "T2" 10 2)
                           (simple-task "t1" "T1" 10 1)]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        ;; Both start at 0, so sorted by task_id: t1 before t2
        (assert (= "t1" (:task-id (assignment-at r 0))))
        (assert (= "t2" (:task-id (assignment-at r 1)))))))

  (run-test "five independent tasks three workers"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)
                           (simple-task "t3" "T3" 10 1)
                           (simple-task "t4" "T4" 10 1)
                           (simple-task "t5" "T5" 10 1)]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")
                           (simple-worker "w3" "W3")]
                          [])]
        (assert (ok? r))
        (assert (= 5 (count (assignments r))))
        ;; First 3 at t=0, next 2 at t=10
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1))))
        (assert (= 0 (:start-time (assignment-at r 2))))
        (assert (= 10 (:start-time (assignment-at r 3))))
        (assert (= 10 (:start-time (assignment-at r 4)))))))

  (run-test "task with deadline that is achievable"
    (fn []
      (let [r (s/schedule [(task-with-deadline "t1" "T1" 10 1 100)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:end-time (assignment-at r 0)))))))

  (run-test "attempt is always 1 on first try"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (= 1 (:attempt (assignment-at r 0)))))))

  (run-test "dependency chain with 2 workers parallelizes independent steps"
    (fn []
      (let [;; A has no deps. B and C both depend on A. D depends on B and C.
            ;; With 2 workers: A at 0-10, B and C parallel at 10-20, D at 20-30
            ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["A"])
            td (task-with-deps "D" "D" 10 1 ["B" "C"])
            r (s/schedule [ta tb tc td]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        (assert (= 4 (count (assignments r))))
        ;; A at 0-10
        (let [aa (first (filter #(= "A" (:task-id %)) (assignments r)))]
          (assert (= 0 (:start-time aa)))
          (assert (= 10 (:end-time aa))))
        ;; B and C both at 10-20 (parallel, different workers)
        (let [ab (first (filter #(= "B" (:task-id %)) (assignments r)))
              ac (first (filter #(= "C" (:task-id %)) (assignments r)))]
          (assert (= 10 (:start-time ab)))
          (assert (= 10 (:start-time ac)))
          (assert (not= (:worker-id ab) (:worker-id ac))))
        ;; D at 20-30
        (let [ad (first (filter #(= "D" (:task-id %)) (assignments r)))]
          (assert (= 20 (:start-time ad)))
          (assert (= 30 (:end-time ad)))))))

  (run-test "varying durations with single worker"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 5 1)
                           (simple-task "t2" "T2" 15 1)
                           (simple-task "t3" "T3" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 5 (:end-time (assignment-at r 0))))
        (assert (= 5 (:start-time (assignment-at r 1))))
        (assert (= 20 (:end-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2))))
        (assert (= 30 (:end-time (assignment-at r 2)))))))

  (run-test "mixed priorities with parallel workers"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 3)
                           (simple-task "t2" "T2" 10 1)
                           (simple-task "t3" "T3" 10 2)]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        ;; Priority 1 (t2) and priority 2 (t3) run parallel at t=0
        ;; Priority 3 (t1) runs at t=10
        (let [at2 (first (filter #(= "t2" (:task-id %)) (assignments r)))
              at3 (first (filter #(= "t3" (:task-id %)) (assignments r)))
              at1 (first (filter #(= "t1" (:task-id %)) (assignments r)))]
          (assert (= 0 (:start-time at2)))
          (assert (= 0 (:start-time at3)))
          (assert (= 10 (:start-time at1)))))))

  (run-test "single task with various durations"
    (fn []
      (let [r1 (s/schedule [(simple-task "t1" "T1" 1 1)] [(simple-worker "w1" "W1")] [])
            r60 (s/schedule [(simple-task "t1" "T1" 60 1)] [(simple-worker "w1" "W1")] [])
            r0 (s/schedule [(simple-task "t1" "T1" 0 1)] [(simple-worker "w1" "W1")] [])]
        (assert (= 1 (:end-time (assignment-at r1 0))))
        (assert (= 60 (:end-time (assignment-at r60 0))))
        (assert (= 0 (:end-time (assignment-at r0 0)))))))

  (run-test "four workers four tasks all parallel"
    (fn []
      (let [r (s/schedule [(simple-task "t1" "T1" 10 1)
                           (simple-task "t2" "T2" 10 1)
                           (simple-task "t3" "T3" 10 1)
                           (simple-task "t4" "T4" 10 1)]
                          [(simple-worker "w1" "W1")
                           (simple-worker "w2" "W2")
                           (simple-worker "w3" "W3")
                           (simple-worker "w4" "W4")]
                          [])]
        (assert (ok? r))
        (assert (= 4 (count (assignments r))))
        ;; All start at 0
        (assert (every? #(= 0 (:start-time %)) (assignments r)))
        ;; All end at 10
        (assert (every? #(= 10 (:end-time %)) (assignments r)))
        ;; Workers assigned in id order
        (assert (= "w1" (:worker-id (assignment-at r 0))))
        (assert (= "w2" (:worker-id (assignment-at r 1))))
        (assert (= "w3" (:worker-id (assignment-at r 2))))
        (assert (= "w4" (:worker-id (assignment-at r 3)))))))

  (run-test "task with zero deadline treated as no deadline"
    (fn []
      (let [r (s/schedule [(task-with-deadline "t1" "T1" 10 1 0)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0)))))))

  (run-test "dependency with different durations"
    (fn []
      (let [ta (simple-task "A" "A" 5 1)
            tb (task-with-deps "B" "B" 20 1 ["A"])
            r (s/schedule [ta tb] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 5 (:start-time (assignment-at r 1))))
        (assert (= 25 (:end-time (assignment-at r 1))))))))


;; =============================================================================
;; 2. Dependency Graph Tests (~50 assertions)
;; =============================================================================

(defn dependency-graph-tests []
  (println "\n--- Dependency Graph ---")

  (run-test "linear chain A->B->C"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["B"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 3 (count (assignments r))))
        (assert (= "A" (:task-id (assignment-at r 0))))
        (assert (= "B" (:task-id (assignment-at r 1))))
        (assert (= "C" (:task-id (assignment-at r 2))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2)))))))

  (run-test "linear chain respects end times"
    (fn []
      (let [ta (simple-task "A" "A" 5 1)
            tb (task-with-deps "B" "B" 15 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["B"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 5 (:start-time (assignment-at r 1))))
        (assert (= 20 (:end-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2))))
        (assert (= 30 (:end-time (assignment-at r 2)))))))

  (run-test "diamond dependency A->B,C->D"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["A"])
            td (task-with-deps "D" "D" 10 1 ["B" "C"])
            r (s/schedule [ta tb tc td] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 4 (count (assignments r))))
        ;; With 1 worker: A(0-10), B(10-20), C(20-30), D(30-40)
        (assert (= "A" (:task-id (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= "D" (:task-id (assignment-at r 3))))
        (assert (= 30 (:start-time (assignment-at r 3))))
        (assert (= 40 (:end-time (assignment-at r 3)))))))

  (run-test "diamond dependency with 2 workers"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["A"])
            td (task-with-deps "D" "D" 10 1 ["B" "C"])
            r (s/schedule [ta tb tc td]
                          [(simple-worker "w1" "W1") (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        ;; A(0-10), B and C parallel (10-20), D(20-30)
        (let [ad (first (filter #(= "D" (:task-id %)) (assignments r)))]
          (assert (= 20 (:start-time ad)))
          (assert (= 30 (:end-time ad)))))))

  (run-test "simple cycle A->B->A"
    (fn []
      (let [ta (task-with-deps "A" "A" 10 1 ["B"])
            tb (task-with-deps "B" "B" 10 1 ["A"])
            r (s/schedule [ta tb] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        (assert (= 1 (count (failures r))))
        (let [f (failure-at r 0)]
          (assert (= "A" (:task-id f)))
          (assert (instance? scheduler.types.DependencyCycle (reason f)))
          (assert (= ["A" "B" "A"] (:cycle (reason f))))))))

  (run-test "complex cycle A->B->C->A"
    (fn []
      (let [ta (task-with-deps "A" "A" 10 1 ["C"])
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["B"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        (assert (= 1 (count (failures r))))
        (let [f (failure-at r 0)
              cyc (:cycle (reason f))]
          (assert (instance? scheduler.types.DependencyCycle (reason f)))
          ;; Cycle is normalized: smallest id first
          (assert (= "A" (first cyc)))
          (assert (= "A" (last cyc)))
          (assert (= 4 (count cyc)))))))

  (run-test "self-dependency detected"
    (fn []
      (let [ta (task-with-deps "A" "A" 10 1 ["A"])
            r (s/schedule [ta] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        (assert (= 1 (count (failures r))))
        (let [f (failure-at r 0)]
          (assert (instance? scheduler.types.DependencyCycle (reason f)))
          (assert (= ["A" "A"] (:cycle (reason f))))))))

  (run-test "multiple disjoint cycles detected"
    (fn []
      (let [ta (task-with-deps "A" "A" 10 1 ["B"])
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["D"])
            td (task-with-deps "D" "D" 10 1 ["C"])
            r (s/schedule [ta tb tc td] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        (assert (= 2 (count (failures r))))
        ;; Both cycles detected
        (let [cycles (mapv #(:cycle (reason %)) (failures r))]
          (assert (= ["A" "B" "A"] (first cycles)))
          (assert (= ["C" "D" "C"] (second cycles)))))))

  (run-test "cycle with non-cyclic tasks still fails entirely"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["C"])
            tc (task-with-deps "C" "C" 10 1 ["B"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        ;; Even though A has no cycle, the cycle in B-C causes entire schedule to fail
        (assert (>= (count (failures r)) 1))
        (let [reasons (mapv #(type (reason %)) (failures r))]
          (assert (some #(= scheduler.types.DependencyCycle %) reasons))))))

  (run-test "long chain of 5 tasks"
    (fn []
      (let [t1 (simple-task "t1" "T1" 5 1)
            t2 (task-with-deps "t2" "T2" 5 1 ["t1"])
            t3 (task-with-deps "t3" "T3" 5 1 ["t2"])
            t4 (task-with-deps "t4" "T4" 5 1 ["t3"])
            t5 (task-with-deps "t5" "T5" 5 1 ["t4"])
            r (s/schedule [t1 t2 t3 t4 t5] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 5 (count (assignments r))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 5 (:start-time (assignment-at r 1))))
        (assert (= 10 (:start-time (assignment-at r 2))))
        (assert (= 15 (:start-time (assignment-at r 3))))
        (assert (= 20 (:start-time (assignment-at r 4))))
        (assert (= 25 (:end-time (assignment-at r 4)))))))

  (run-test "dependency respected across different priorities"
    (fn []
      (let [;; B (priority 1) depends on A (priority 2). A must run first despite lower priority.
            ta (simple-task "A" "A" 10 2)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            r (s/schedule [ta tb] [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= "A" (:task-id (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= "B" (:task-id (assignment-at r 1))))
        (assert (= 10 (:start-time (assignment-at r 1)))))))

  (run-test "multiple roots no deps sorted by priority then id"
    (fn []
      (let [r (s/schedule [(simple-task "Z" "Z" 10 1)
                           (simple-task "A" "A" 10 1)
                           (simple-task "M" "M" 10 1)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        (assert (= "A" (:task-id (assignment-at r 0))))
        (assert (= "M" (:task-id (assignment-at r 1))))
        (assert (= "Z" (:task-id (assignment-at r 2)))))))

  (run-test "two separate chains no interference"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (simple-task "C" "C" 10 2)
            td (task-with-deps "D" "D" 10 2 ["C"])
            r (s/schedule [ta tb tc td]
                          [(simple-worker "w1" "W1") (simple-worker "w2" "W2")]
                          [])]
        (assert (ok? r))
        (assert (= 4 (count (assignments r))))
        ;; A and C both at 0 (parallel, different priorities but 2 workers)
        (let [aa (first (filter #(= "A" (:task-id %)) (assignments r)))
              ac (first (filter #(= "C" (:task-id %)) (assignments r)))]
          (assert (= 0 (:start-time aa)))
          (assert (= 0 (:start-time ac)))))))

  (run-test "cycle path is normalized with smallest id first"
    (fn []
      (let [;; Cycle: C->B->A->C. Normalized should start with A.
            ta (task-with-deps "C" "C" 10 1 ["B"])
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "A" "A" 10 1 ["C"])
            r (s/schedule [ta tb tc] [(simple-worker "w1" "W1")] [])]
        (assert (err? r))
        (let [cyc (:cycle (reason (failure-at r 0)))]
          (assert (= "A" (first cyc)))
          (assert (= "A" (last cyc)))))))

  (run-test "fan-out dependency: one task feeds many"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["A"])
            td (task-with-deps "D" "D" 10 1 ["A"])
            r (s/schedule [ta tb tc td]
                          [(simple-worker "w1" "W1") (simple-worker "w2" "W2") (simple-worker "w3" "W3")]
                          [])]
        (assert (ok? r))
        ;; A at 0-10, B/C/D all at 10-20 parallel
        (let [ab (first (filter #(= "B" (:task-id %)) (assignments r)))
              ac (first (filter #(= "C" (:task-id %)) (assignments r)))
              ad (first (filter #(= "D" (:task-id %)) (assignments r)))]
          (assert (= 10 (:start-time ab)))
          (assert (= 10 (:start-time ac)))
          (assert (= 10 (:start-time ad)))))))

  (run-test "fan-in dependency: many tasks feed one"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (simple-task "B" "B" 15 1)
            tc (simple-task "C" "C" 5 1)
            td (task-with-deps "D" "D" 10 1 ["A" "B" "C"])
            r (s/schedule [ta tb tc td]
                          [(simple-worker "w1" "W1") (simple-worker "w2" "W2") (simple-worker "w3" "W3")]
                          [])]
        (assert (ok? r))
        ;; D starts after the latest dependency ends. B ends at 15.
        (let [ad (first (filter #(= "D" (:task-id %)) (assignments r)))]
          (assert (= 15 (:start-time ad)))
          (assert (= 25 (:end-time ad))))))))


;; =============================================================================
;; 3. Capabilities Tests (~40 assertions)
;; =============================================================================

(defn capability-tests []
  (println "\n--- Capabilities ---")

  (run-test "exact capability match"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["welding"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "superset capability match"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["welding" "painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "multiple required capabilities all present"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding" "painting"])
            w1 (worker-with-caps "w1" "W1" ["welding" "painting" "plumbing"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "missing single capability rejection"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (let [f (failure-at r 0)]
          (assert (instance? scheduler.types.NoCapableWorker (reason f)))
          (assert (= ["welding"] (:required (reason f))))))))

  (run-test "missing one of multiple required capabilities"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding" "painting"])
            w1 (worker-with-caps "w1" "W1" ["welding"])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 0))))
        (assert (= ["welding" "painting"] (:required (reason (failure-at r 0))))))))

  (run-test "no capable worker error includes available capabilities"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["painting"])
            w2 (worker-with-caps "w2" "W2" ["plumbing"])
            r (s/schedule [t1] [w1 w2] [])]
        (assert (err? r))
        (let [f (failure-at r 0)
              avail (:available (reason f))]
          (assert (= 2 (count avail)))))))

  (run-test "no-cap task matches any worker"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-caps "w1" "W1" ["painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "capable worker selected over incapable by id"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["painting"])
            w2 (worker-with-caps "w2" "W2" ["welding"])
            r (s/schedule [t1] [w1 w2] [])]
        (assert (ok? r))
        (assert (= "w2" (:worker-id (assignment-at r 0)))))))

  (run-test "multiple capable workers picks lowest id"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w3" "W3" ["welding"])
            w2 (worker-with-caps "w1" "W1" ["welding"])
            w3 (worker-with-caps "w2" "W2" ["welding"])
            r (s/schedule [t1] [w1 w2 w3] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "two tasks different capabilities routed correctly"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            t2 (task-with-caps "t2" "T2" 10 1 ["painting"])
            w1 (worker-with-caps "w1" "W1" ["welding"])
            w2 (worker-with-caps "w2" "W2" ["painting"])
            r (s/schedule [t1 t2] [w1 w2] [])]
        (assert (ok? r))
        (let [a1 (first (filter #(= "t1" (:task-id %)) (assignments r)))
              a2 (first (filter #(= "t2" (:task-id %)) (assignments r)))]
          (assert (= "w1" (:worker-id a1)))
          (assert (= "w2" (:worker-id a2)))))))

  (run-test "task with empty caps matches worker with caps"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-caps "w1" "W1" ["welding" "painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "three capabilities all required"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["a" "b" "c"])
            w1 (worker-with-caps "w1" "W1" ["a" "b" "c"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= "w1" (:worker-id (assignment-at r 0)))))))

  (run-test "worker with extra caps still matches"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["a"])
            w1 (worker-with-caps "w1" "W1" ["a" "b" "c" "d"])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r)))))

  (run-test "two tasks competing for one capable worker serialize"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            t2 (task-with-caps "t2" "T2" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["welding"])
            w2 (worker-with-caps "w2" "W2" ["painting"])
            r (s/schedule [t1 t2] [w1 w2] [])]
        (assert (ok? r))
        ;; Both assigned to w1 (only capable worker), serialized
        (let [a1 (first (filter #(= "t1" (:task-id %)) (assignments r)))
              a2 (first (filter #(= "t2" (:task-id %)) (assignments r)))]
          (assert (= "w1" (:worker-id a1)))
          (assert (= "w1" (:worker-id a2)))
          (assert (= 0 (:start-time a1)))
          (assert (= 10 (:start-time a2)))))))

  (run-test "no capable worker error task-id"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (worker-with-caps "w1" "W1" ["painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (assert (= "t1" (:task-id (failure-at r 0)))))))

  (run-test "all workers have partial caps but none has all"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["a" "b"])
            w1 (worker-with-caps "w1" "W1" ["a"])
            w2 (worker-with-caps "w2" "W2" ["b"])
            r (s/schedule [t1] [w1 w2] [])]
        (assert (err? r))
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 0))))))))


;; =============================================================================
;; 4. Resources Tests (~40 assertions)
;; =============================================================================

(defn resource-tests []
  (println "\n--- Resources ---")

  (run-test "resource capacity 1 serializes two tasks"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1 w2] [res])]
        (assert (ok? r))
        (assert (= 2 (count (assignments r))))
        ;; Both use same worker (w1) since resource serializes them
        (let [a1 (assignment-at r 0)
              a2 (assignment-at r 1)]
          (assert (= 0 (:start-time a1)))
          (assert (= 10 (:end-time a1)))
          (assert (= 10 (:start-time a2)))
          (assert (= 20 (:end-time a2)))))))

  (run-test "resource capacity 2 allows parallelism"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 2)
            r (s/schedule [t1 t2] [w1 w2] [res])]
        (assert (ok? r))
        ;; Both start at 0 because capacity allows 2 concurrent
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1))))
        (assert (= "w1" (:worker-id (assignment-at r 0))))
        (assert (= "w2" (:worker-id (assignment-at r 1)))))))

  (run-test "resource capacity 2 with 3 tasks forces third to slide"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            t3 (task-with-resources "t3" "T3" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            w3 (simple-worker "w3" "W3")
            res (resource "r1" "R1" 2)
            r (s/schedule [t1 t2 t3] [w1 w2 w3] [res])]
        (assert (ok? r))
        (assert (= 3 (count (assignments r))))
        ;; First two at t=0, third slides to t=10
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1))))
        (assert (= 10 (:start-time (assignment-at r 2))))
        (assert (= 20 (:end-time (assignment-at r 2)))))))

  (run-test "multiple resources both cap 1"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1" "r2"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            r1 (resource "r1" "R1" 1)
            r2 (resource "r2" "R2" 1)
            r (s/schedule [t1 t2] [w1 w2] [r1 r2])]
        (assert (ok? r))
        ;; t1 uses both r1 and r2 at t=0. t2 needs r1, must wait until t=10.
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1)))))))

  (run-test "resource cap 1 three tasks same worker serialized"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            t3 (task-with-resources "t3" "T3" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            w3 (simple-worker "w3" "W3")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2 t3] [w1 w2 w3] [res])]
        (assert (ok? r))
        ;; All serialized on same worker due to resource constraint
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1))))
        (assert (= 20 (:start-time (assignment-at r 2)))))))

  (run-test "task with no resources ignores resource constraints"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (simple-task "t2" "T2" 10 1)
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1 w2] [res])]
        (assert (ok? r))
        ;; Both parallel despite cap 1 resource (neither uses it)
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1)))))))

  (run-test "resource constraint with dependencies"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] ["r1"] [] (t/->RetryPolicy 0 0))
            t2 (t/->Task "t2" "T2" 10 1 0 [] ["r1"] ["t1"] (t/->RetryPolicy 0 0))
            w1 (simple-worker "w1" "W1")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1] [res])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:start-time (assignment-at r 1)))))))

  (run-test "high capacity resource allows full parallelism"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            t3 (task-with-resources "t3" "T3" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            w3 (simple-worker "w3" "W3")
            res (resource "r1" "R1" 10)
            r (s/schedule [t1 t2 t3] [w1 w2 w3] [res])]
        (assert (ok? r))
        (assert (every? #(= 0 (:start-time %)) (assignments r))))))

  (run-test "two different resources each cap 1"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r2"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            r1 (resource "r1" "R1" 1)
            r2 (resource "r2" "R2" 1)
            r (s/schedule [t1 t2] [w1 w2] [r1 r2])]
        (assert (ok? r))
        ;; Different resources, so parallel is fine
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1)))))))

  (run-test "resource cap 1 with varying durations"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 5 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1 w2] [res])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 5 (:end-time (assignment-at r 0))))
        ;; t2 slides to when r1 is free (t=5)
        (assert (= 5 (:start-time (assignment-at r 1))))
        (assert (= 15 (:end-time (assignment-at r 1)))))))

  (run-test "resource cap 3 allows three parallel"
    (fn []
      (let [tasks (mapv #(task-with-resources (str "t" %) (str "T" %) 10 1 ["r1"]) (range 1 4))
            workers (mapv #(simple-worker (str "w" %) (str "W" %)) (range 1 4))
            res (resource "r1" "R1" 3)
            r (s/schedule tasks workers [res])]
        (assert (ok? r))
        (assert (every? #(= 0 (:start-time %)) (assignments r)))))))


;; =============================================================================
;; 5. Time Windows Tests (~40 assertions)
;; =============================================================================

(defn time-window-tests []
  (println "\n--- Time Windows ---")

  (run-test "unavailable window avoidance"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 15)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (let [a (assignment-at r 0)]
          (assert (= 15 (:start-time a)))
          (assert (= 25 (:end-time a)))))))

  (run-test "task fits in gap between windows"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 5) (window 20 30)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 5 (:start-time (assignment-at r 0))))
        (assert (= 15 (:end-time (assignment-at r 0)))))))

  (run-test "all-day unavailability routes to second worker"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 1440)])
            w2 (simple-worker "w2" "W2")
            r (s/schedule [t1] [w1 w2] [])]
        (assert (ok? r))
        (assert (= "w2" (:worker-id (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0)))))))

  (run-test "deadline plus window causes deadline missed"
    (fn []
      (let [;; Worker unavail 0-15, task dur=10, deadline=20.
            ;; Earliest available at 15, end=25 > deadline=20. Fail.
            t1 (t/->Task "t1" "T1" 10 1 20 [] [] [] (t/->RetryPolicy 0 0))
            w1 (worker-with-windows "w1" "W1" [(window 0 15)])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (let [f (failure-at r 0)]
          ;; The scheduler detects this as unavailable since it can't fit before deadline
          (assert (or (instance? scheduler.types.DeadlineMissed (reason f))
                      (instance? scheduler.types.WorkerUnavailable (reason f))))))))

  (run-test "worker available after window ends"
    (fn []
      (let [t1 (simple-task "t1" "T1" 5 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 100)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 100 (:start-time (assignment-at r 0))))
        (assert (= 105 (:end-time (assignment-at r 0)))))))

  (run-test "multiple unavailable windows on same worker"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            ;; Windows: (0,5), (10,20), (30,40). Task dur=10.
            ;; At t=5, end=15 overlaps (10,20). At t=20, end=30, no overlap with (30,40).
            w1 (worker-with-windows "w1" "W1" [(window 0 5) (window 10 20) (window 30 40)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 20 (:start-time (assignment-at r 0))))
        (assert (= 30 (:end-time (assignment-at r 0)))))))

  (run-test "window exactly at task boundary allows scheduling"
    (fn []
      (let [;; Worker unavail 0-10. Task dur=10. Should start at 10.
            t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 10)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 10 (:start-time (assignment-at r 0)))))))

  (run-test "tight deadline just barely fits"
    (fn []
      (let [;; Task dur=10, deadline=10. Start at 0, end=10 <= deadline. OK.
            t1 (task-with-deadline "t1" "T1" 10 1 10)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:end-time (assignment-at r 0)))))))

  (run-test "deadline too tight fails"
    (fn []
      (let [;; Task dur=10, deadline=5. Start at 0, end=10 > deadline=5. Fail.
            t1 (task-with-deadline "t1" "T1" 10 1 5)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (err? r)))))

  (run-test "two workers one unavailable picks available"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 100)])
            w2 (simple-worker "w2" "W2")
            r (s/schedule [t1] [w1 w2] [])]
        (assert (ok? r))
        (assert (= "w2" (:worker-id (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0)))))))

  (run-test "window and dependency interaction"
    (fn []
      (let [;; A runs 0-10. B depends on A, worker unavail 10-20.
            ;; B starts at 20.
            ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            w1 (worker-with-windows "w1" "W1" [(window 10 20)])
            r (s/schedule [ta tb] [w1] [])]
        (assert (ok? r))
        (let [ab (first (filter #(= "B" (:task-id %)) (assignments r)))]
          (assert (= 20 (:start-time ab)))
          (assert (= 30 (:end-time ab)))))))

  (run-test "worker unavailable exactly during task window slides past"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            ;; Unavailable exactly at [0, 10). Task must start at 10.
            w1 (worker-with-windows "w1" "W1" [(window 0 10)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 10 (:start-time (assignment-at r 0)))))))

  (run-test "late window does not affect early task"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            ;; Unavailable at [100, 200). Task at 0-10, no conflict.
            w1 (worker-with-windows "w1" "W1" [(window 100 200)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:end-time (assignment-at r 0)))))))

  (run-test "two tasks two workers one partially unavailable"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (simple-task "t2" "T2" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 5)])
            w2 (simple-worker "w2" "W2")
            r (s/schedule [t1 t2] [w1 w2] [])]
        (assert (ok? r))
        ;; w2 available at 0, gets t1. w1 available at 5, gets t2 at 5.
        (let [a1 (first (filter #(= "t1" (:task-id %)) (assignments r)))
              a2 (first (filter #(= "t2" (:task-id %)) (assignments r)))]
          (assert (= "w2" (:worker-id a1)))
          (assert (= 0 (:start-time a1)))
          (assert (= 5 (:start-time a2)))))))

  (run-test "deadline exactly at end time is OK"
    (fn []
      (let [;; dur=10, deadline=10, start=0, end=10 <= 10
            t1 (task-with-deadline "t1" "T1" 10 1 10)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:end-time (assignment-at r 0)))))))

  (run-test "deadline off by one fails"
    (fn []
      (let [;; dur=10, deadline=9, start=0, end=10 > 9
            t1 (task-with-deadline "t1" "T1" 10 1 9)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (err? r)))))

  (run-test "adjacent windows leave no gap"
    (fn []
      (let [;; Windows [0,10) and [10,20) cover 0-20 entirely
            t1 (simple-task "t1" "T1" 5 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 10) (window 10 20)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 20 (:start-time (assignment-at r 0)))))))

  (run-test "window caps and dependency combined"
    (fn []
      (let [ta (task-with-caps "A" "A" 10 1 ["welding"])
            tb (t/->Task "B" "B" 10 1 0 ["welding"] [] ["A"] (t/->RetryPolicy 0 0))
            w1 (t/->Worker "w1" "W1" ["welding"] [(window 10 25)])
            r (s/schedule [ta tb] [w1] [])]
        (assert (ok? r))
        ;; A at 0-10. B depends on A, starts at 10 but worker unavail 10-25.
        ;; B starts at 25.
        (let [ab (first (filter #(= "B" (:task-id %)) (assignments r)))]
          (assert (= 25 (:start-time ab)))
          (assert (= 35 (:end-time ab))))))))


;; =============================================================================
;; 6. Retry Tests (~30 assertions)
;; =============================================================================

(defn retry-tests []
  (println "\n--- Retry ---")

  (run-test "retry succeeds on second attempt with backoff"
    (fn []
      (let [;; Worker unavail 0-1441. max-slide from t=0 is 1440 (0..1440).
            ;; Attempt 1: slide from 0 to 1440, all unavail. FAIL.
            ;; Attempt 2: start = 0 + 10 = 10, slide from 10 to 1450, find t=1441.
            t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 10))
            w1 (worker-with-windows "w1" "W1" [(window 0 1441)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (let [a (assignment-at r 0)]
          (assert (= 1441 (:start-time a)))
          (assert (= 1446 (:end-time a)))
          (assert (= 2 (:attempt a)))))))

  (run-test "retry attempt count tracks correctly"
    (fn []
      (let [;; Worker unavail 0-1445.
            ;; Attempt 1: slide 0..1440, fail.
            ;; Attempt 2: slide 10..1450, find 1445. Success at attempt 2.
            t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 10))
            w1 (worker-with-windows "w1" "W1" [(window 0 1445)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 2 (:attempt (assignment-at r 0))))
        (assert (= 1445 (:start-time (assignment-at r 0)))))))

  (run-test "all retries exhausted returns failure"
    (fn []
      (let [;; Worker unavail 0-2000. 3 attempts, backoff 10.
            ;; Attempt 1: 0..1440, fail.
            ;; Attempt 2: 10..1450, fail.
            ;; Attempt 3: 20..1460, fail. All exhausted.
            t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 10))
            w1 (worker-with-windows "w1" "W1" [(window 0 2000)])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (assert (= 1 (count (failures r))))
        (assert (= "t1" (:task-id (failure-at r 0)))))))

  (run-test "backoff past deadline causes failure"
    (fn []
      (let [;; Worker unavail 0-25. Task dur=10, deadline=30.
            ;; Attempt 1: slide from 0, skip unavail to 25. end=35 > deadline 30. Fail.
            ;; Attempt 2: start=0+20=20, slide from 20, still unavail until 25.
            ;;   at 25, end=35 > deadline=30. Fail.
            ;; Attempt 3: start=20+20=40, but 40 > deadline=30. Fail.
            t1 (t/->Task "t1" "T1" 10 1 30 [] [] [] (t/->RetryPolicy 3 20))
            w1 (worker-with-windows "w1" "W1" [(window 0 25)])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r)))))

  (run-test "max_attempts=0 means try once (no retry)"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 0 5))
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 1 (:attempt (assignment-at r 0)))))))

  (run-test "max_attempts=1 same as no retry"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 1 5))
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 1 (:attempt (assignment-at r 0)))))))

  (run-test "retry with no backoff fails immediately after first attempt"
    (fn []
      (let [;; Retry with backoff=0 and max_attempts=3. Since backoff=0,
            ;; the code path goes to failure after first failed attempt.
            t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 0))
            w1 (worker-with-windows "w1" "W1" [(window 0 2000)])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r)))))

  (run-test "successful first attempt does not retry"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 5 10))
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 1 (:attempt (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0)))))))

  (run-test "retry with large backoff reaches available window"
    (fn []
      (let [;; Worker unavail 0-1441. Backoff=100.
            ;; Attempt 1: slide 0..1440, fail.
            ;; Attempt 2: start=100, slide 100..1540, find 1441. Success.
            t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 100))
            w1 (worker-with-windows "w1" "W1" [(window 0 1441)])
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 2 (:attempt (assignment-at r 0))))
        (assert (= 1441 (:start-time (assignment-at r 0)))))))

  (run-test "retry policy on normal successful task is transparent"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 10 100))
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 1 (:attempt (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 10 (:end-time (assignment-at r 0)))))))

  (run-test "retry with capability mismatch always fails"
    (fn []
      (let [;; No capable worker means retry cannot help
            t1 (t/->Task "t1" "T1" 10 1 0 ["welding"] [] [] (t/->RetryPolicy 5 10))
            w1 (worker-with-caps "w1" "W1" ["painting"])
            r (s/schedule [t1] [w1] [])]
        (assert (err? r))
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 0))))))))


;; =============================================================================
;; 7. Edge Cases Tests (~20 assertions)
;; =============================================================================

(defn edge-case-tests []
  (println "\n--- Edge Cases ---")

  (run-test "zero-duration task"
    (fn []
      (let [t1 (simple-task "t1" "T1" 0 1)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:end-time (assignment-at r 0))))
        (assert (= 1 (:attempt (assignment-at r 0)))))))

  (run-test "zero-duration task followed by normal task"
    (fn []
      (let [t1 (simple-task "t1" "T1" 0 1)
            t2 (task-with-deps "t2" "T2" 10 1 ["t1"])
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1 t2] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:end-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1))))
        (assert (= 10 (:end-time (assignment-at r 1)))))))

  (run-test "no workers causes all tasks to fail"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (simple-task "t2" "T2" 10 1)
            r (s/schedule [t1 t2] [] [])]
        (assert (err? r))
        (assert (= 2 (count (failures r))))
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 0))))
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 1)))))))

  (run-test "no workers single task failure"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            r (s/schedule [t1] [] [])]
        (assert (err? r))
        (assert (= 1 (count (failures r))))
        (assert (= "t1" (:task-id (failure-at r 0)))))))

  (run-test "dependency failure propagation"
    (fn []
      (let [;; t1 can't be scheduled (no capable worker). t2 depends on t1.
            t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            t2 (task-with-deps "t2" "T2" 10 1 ["t1"])
            w1 (worker-with-caps "w1" "W1" ["painting"])
            r (s/schedule [t1 t2] [w1] [])]
        (assert (err? r))
        (assert (= 2 (count (failures r))))
        ;; t1 fails due to no capable worker
        (assert (instance? scheduler.types.NoCapableWorker (reason (failure-at r 0))))
        ;; t2 fails due to dependency failure propagation
        (assert (= "t2" (:task-id (failure-at r 1)))))))

  (run-test "large duration task"
    (fn []
      (let [t1 (simple-task "t1" "T1" 1000 1)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])]
        (assert (ok? r))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 1000 (:end-time (assignment-at r 0)))))))

  (run-test "many tasks same priority ordered by id"
    (fn []
      (let [tasks (mapv #(simple-task (str "t" %) (str "T" %) 1 1) (range 1 6))
            r (s/schedule tasks [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 5 (count (assignments r))))
        (assert (= "t1" (:task-id (assignment-at r 0))))
        (assert (= "t2" (:task-id (assignment-at r 1))))
        (assert (= "t3" (:task-id (assignment-at r 2))))
        (assert (= "t4" (:task-id (assignment-at r 3))))
        (assert (= "t5" (:task-id (assignment-at r 4)))))))

  (run-test "single worker multiple tasks with various priorities"
    (fn []
      (let [r (s/schedule [(simple-task "a" "A" 5 5)
                           (simple-task "b" "B" 5 3)
                           (simple-task "c" "C" 5 1)
                           (simple-task "d" "D" 5 4)
                           (simple-task "e" "E" 5 2)]
                          [(simple-worker "w1" "W1")]
                          [])]
        (assert (ok? r))
        ;; Ordered by priority ASC: c(1), e(2), b(3), d(4), a(5)
        (assert (= "c" (:task-id (assignment-at r 0))))
        (assert (= "e" (:task-id (assignment-at r 1))))
        (assert (= "b" (:task-id (assignment-at r 2))))
        (assert (= "d" (:task-id (assignment-at r 3))))
        (assert (= "a" (:task-id (assignment-at r 4))))
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 20 (:start-time (assignment-at r 4)))))))

  (run-test "two zero-duration tasks same worker"
    (fn []
      (let [t1 (simple-task "t1" "T1" 0 1)
            t2 (simple-task "t2" "T2" 0 1)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1 t2] [w1] [])]
        (assert (ok? r))
        (assert (= 2 (count (assignments r))))
        ;; Both zero-duration, both at t=0
        (assert (= 0 (:start-time (assignment-at r 0))))
        (assert (= 0 (:end-time (assignment-at r 0))))
        (assert (= 0 (:start-time (assignment-at r 1))))
        (assert (= 0 (:end-time (assignment-at r 1)))))))

  (run-test "mixed: caps + resources + deps + windows"
    (fn []
      (let [ta (t/->Task "A" "A" 10 1 0 ["welding"] ["r1"] [] (t/->RetryPolicy 0 0))
            tb (t/->Task "B" "B" 10 1 0 ["welding"] ["r1"] ["A"] (t/->RetryPolicy 0 0))
            w1 (t/->Worker "w1" "W1" ["welding"] [(window 10 15)])
            res (resource "r1" "R1" 1)
            r (s/schedule [ta tb] [w1] [res])]
        (assert (ok? r))
        ;; A at 0-10. B depends on A and needs r1 (cap 1, freed at 10).
        ;; B earliest start is 10 (dep on A). But worker unavail 10-15. Slides to 15.
        (let [ab (first (filter #(= "B" (:task-id %)) (assignments r)))]
          (assert (= 15 (:start-time ab)))
          (assert (= 25 (:end-time ab)))))))

  (run-test "ten tasks one worker sequential"
    (fn []
      (let [tasks (mapv #(simple-task (str "t" (format "%02d" %)) (str "T" %) 5 1) (range 1 11))
            r (s/schedule tasks [(simple-worker "w1" "W1")] [])]
        (assert (ok? r))
        (assert (= 10 (count (assignments r))))
        ;; Last task starts at 45, ends at 50
        (assert (= 45 (:start-time (assignment-at r 9))))
        (assert (= 50 (:end-time (assignment-at r 9))))))))


;; =============================================================================
;; 8. Validator Tests (~20+ assertions)
;; =============================================================================

(defn validator-tests []
  (println "\n--- Validator ---")

  (run-test "validator on correct single-task schedule returns no violations"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1] [w1] [])
            violations (v/validate-assignments (assignments r) [t1] [w1] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct multi-task schedule"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (task-with-deps "t2" "T2" 10 1 ["t1"])
            w1 (simple-worker "w1" "W1")
            r (s/schedule [t1 t2] [w1] [])
            violations (v/validate-assignments (assignments r) [t1 t2] [w1] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct parallel schedule"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (simple-task "t2" "T2" 10 1)
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            r (s/schedule [t1 t2] [w1 w2] [])
            violations (v/validate-assignments (assignments r) [t1 t2] [w1 w2] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator catches capability mismatch"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            w1 (simple-worker "w1" "W1")
            bad-a (t/->Assignment "t1" "w1" 0 10 1)
            violations (v/validate-assignments [bad-a] [t1] [w1] [])]
        (assert (= 1 (count violations)))
        (assert (instance? scheduler.types.CapabilityMismatch (:kind (first violations)))))))

  (run-test "validator catches window conflict"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 5 15)])
            bad-a (t/->Assignment "t1" "w1" 0 10 1)
            violations (v/validate-assignments [bad-a] [t1] [w1] [])]
        (assert (= 1 (count violations)))
        (assert (instance? scheduler.types.WindowConflict (:kind (first violations)))))))

  (run-test "validator catches deadline exceeded"
    (fn []
      (let [t1 (task-with-deadline "t1" "T1" 10 1 5)
            w1 (simple-worker "w1" "W1")
            bad-a (t/->Assignment "t1" "w1" 0 10 1)
            violations (v/validate-assignments [bad-a] [t1] [w1] [])]
        (assert (= 1 (count violations)))
        (assert (instance? scheduler.types.DeadlineExceeded (:kind (first violations)))))))

  (run-test "validator catches overlap violation"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (simple-task "t2" "T2" 10 1)
            w1 (simple-worker "w1" "W1")
            a1 (t/->Assignment "t1" "w1" 0 10 1)
            a2 (t/->Assignment "t2" "w1" 5 15 1)
            violations (v/validate-assignments [a1 a2] [t1 t2] [w1] [])]
        (assert (>= (count violations) 2))
        (let [kinds (set (mapv #(type (:kind %)) violations))]
          (assert (contains? kinds scheduler.types.OverlapViolation))))))

  (run-test "validator catches dependency order violation"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            t2 (task-with-deps "t2" "T2" 10 1 ["t1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            ;; t1 at 10-20, t2 at 5-15 -- t2 starts before t1 ends
            a1 (t/->Assignment "t1" "w1" 10 20 1)
            a2 (t/->Assignment "t2" "w2" 5 15 1)
            violations (v/validate-assignments [a1 a2] [t1 t2] [w1 w2] [])]
        (let [dep-violations (filterv #(instance? scheduler.types.DependencyOrder (:kind %)) violations)]
          (assert (>= (count dep-violations) 1))))))

  (run-test "validator catches resource exceeded"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 1)
            ;; Both at same time, exceeds cap=1
            a1 (t/->Assignment "t1" "w1" 0 10 1)
            a2 (t/->Assignment "t2" "w2" 0 10 1)
            violations (v/validate-assignments [a1 a2] [t1 t2] [w1 w2] [res])]
        (let [res-violations (filterv #(instance? scheduler.types.ResourceExceeded (:kind %)) violations)]
          (assert (>= (count res-violations) 1))))))

  (run-test "validator on correct resource-constrained schedule"
    (fn []
      (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
            t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1 w2] [res])
            violations (v/validate-assignments (assignments r) [t1 t2] [w1 w2] [res])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct diamond schedule"
    (fn []
      (let [ta (simple-task "A" "A" 10 1)
            tb (task-with-deps "B" "B" 10 1 ["A"])
            tc (task-with-deps "C" "C" 10 1 ["A"])
            td (task-with-deps "D" "D" 10 1 ["B" "C"])
            w1 (simple-worker "w1" "W1")
            w2 (simple-worker "w2" "W2")
            r (s/schedule [ta tb tc td] [w1 w2] [])
            violations (v/validate-assignments (assignments r) [ta tb tc td] [w1 w2] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on empty schedule"
    (fn []
      (let [violations (v/validate-assignments [] [] [] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct window-avoidance schedule"
    (fn []
      (let [t1 (simple-task "t1" "T1" 10 1)
            w1 (worker-with-windows "w1" "W1" [(window 0 15)])
            r (s/schedule [t1] [w1] [])
            violations (v/validate-assignments (assignments r) [t1] [w1] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct capability-routed schedule"
    (fn []
      (let [t1 (task-with-caps "t1" "T1" 10 1 ["welding"])
            t2 (task-with-caps "t2" "T2" 10 1 ["painting"])
            w1 (worker-with-caps "w1" "W1" ["welding"])
            w2 (worker-with-caps "w2" "W2" ["painting"])
            r (s/schedule [t1 t2] [w1 w2] [])
            violations (v/validate-assignments (assignments r) [t1 t2] [w1 w2] [])]
        (assert (= 0 (count violations))))))

  (run-test "validator on correct retry schedule"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 5 1 0 [] [] [] (t/->RetryPolicy 3 10))
            w1 (worker-with-windows "w1" "W1" [(window 0 1441)])
            r (s/schedule [t1] [w1] [])]
        (when (ok? r)
          (let [violations (v/validate-assignments (assignments r) [t1] [w1] [])]
            (assert (= 0 (count violations))))))))

  (run-test "validator catches multiple violation types simultaneously"
    (fn []
      (let [;; Cap mismatch AND deadline exceeded
            t1 (t/->Task "t1" "T1" 10 1 5 ["welding"] [] [] (t/->RetryPolicy 0 0))
            w1 (simple-worker "w1" "W1")
            bad-a (t/->Assignment "t1" "w1" 0 10 1)
            violations (v/validate-assignments [bad-a] [t1] [w1] [])]
        (assert (>= (count violations) 2))
        (let [kinds (set (mapv #(type (:kind %)) violations))]
          (assert (contains? kinds scheduler.types.CapabilityMismatch))
          (assert (contains? kinds scheduler.types.DeadlineExceeded))))))

  (run-test "validator on correct chain with resource"
    (fn []
      (let [t1 (t/->Task "t1" "T1" 10 1 0 [] ["r1"] [] (t/->RetryPolicy 0 0))
            t2 (t/->Task "t2" "T2" 10 1 0 [] ["r1"] ["t1"] (t/->RetryPolicy 0 0))
            w1 (simple-worker "w1" "W1")
            res (resource "r1" "R1" 1)
            r (s/schedule [t1 t2] [w1] [res])]
        (assert (ok? r))
        (let [violations (v/validate-assignments (assignments r) [t1 t2] [w1] [res])]
          (assert (= 0 (count violations)))))))

  (run-test "validator on correct five-task priority schedule"
    (fn []
      (let [tasks [(simple-task "a" "A" 5 5)
                   (simple-task "b" "B" 5 3)
                   (simple-task "c" "C" 5 1)
                   (simple-task "d" "D" 5 4)
                   (simple-task "e" "E" 5 2)]
            w1 (simple-worker "w1" "W1")
            r (s/schedule tasks [w1] [])]
        (assert (ok? r))
        (let [violations (v/validate-assignments (assignments r) tasks [w1] [])]
          (assert (= 0 (count violations))))))))


;; =============================================================================
;; Run All Tests
;; =============================================================================

(println "E16 Workflow Scheduler — Verification Oracle")
(println "=============================================")

(happy-path-tests)
(dependency-graph-tests)
(capability-tests)
(resource-tests)
(time-window-tests)
(retry-tests)
(edge-case-tests)
(validator-tests)

;; Summary
(let [results @*results*
      total (count results)
      passed (count (filter identity results))
      failed (- total passed)]
  (println "\n=============================================")
  (println (str passed "/" total " tests passed"))
  (when (> failed 0)
    (println (str failed " test(s) FAILED")))
  (System/exit (if (= passed total) 0 1)))
