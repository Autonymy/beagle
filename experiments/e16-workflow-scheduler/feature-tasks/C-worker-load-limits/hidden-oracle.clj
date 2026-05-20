#!/usr/bin/env bb
;; Feature C: Worker Load Limits — Hidden Tests
;; These measure structural completeness. The agent never sees these.

(require '[scheduler.scheduler :as s]
         '[scheduler.types :as t]
         '[scheduler.validator :as v]
         '[scheduler.errors :as err])

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

(defn simple-task [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0)))

(defn simple-worker [id name]
  (t/->Worker id name [] [] 0))

(defn limited-worker [id name max-tasks]
  (t/->Worker id name [] [] max-tasks))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn failures [result]
  (if (ok? result)
    []
    (.-failures result)))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Hidden Tests ───────────────────────────────────────────────────────

(println "=== Feature C: Worker Load Limits — Hidden Tests ===")
(println "")

;; --- Match completeness: errors.bgl handles WorkerOverloaded ---

(run-test "describe-failure-reason handles WorkerOverloaded"
  (fn []
    (let [wo (t/->WorkerOverloaded "w1" 3)
          desc (err/describe-failure-reason wo)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

(run-test "failure-reason-category handles WorkerOverloaded"
  (fn []
    (let [wo (t/->WorkerOverloaded "w1" 3)
          cat (err/failure-reason-category wo)]
      (assert (= "overload" cat) "WorkerOverloaded category should be 'overload'"))))

(run-test "failure-is-structural? handles WorkerOverloaded"
  (fn []
    (let [wo (t/->WorkerOverloaded "w1" 3)
          structural (err/failure-is-structural? wo)]
      (assert (= false structural) "WorkerOverloaded is not structural"))))

;; --- Error aggregation works with WorkerOverloaded ---

(run-test "group-failures-by-reason includes overload category"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->WorkerOverloaded "w1" 1))
          grouped (err/group-failures-by-reason [sf])]
      (assert (contains? grouped "overload") "should have 'overload' category")
      (assert (= 1 (count (get grouped "overload")))))))

(run-test "summarize-failures includes overloaded failures"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->WorkerOverloaded "w1" 1))
          summary (err/summarize-failures [sf])]
      (assert (string? summary))
      (assert (.contains summary "overload") "summary should mention overload"))))

;; --- Regression: existing FailureReason descriptions still work ---

(run-test "describe-failure-reason still handles DependencyCycle"
  (fn []
    (let [dc (t/->DependencyCycle ["A" "B" "A"])
          desc (err/describe-failure-reason dc)]
      (assert (.contains desc "cycle") "should mention cycle"))))

(run-test "describe-failure-reason still handles DeadlineMissed"
  (fn []
    (let [dm (t/->DeadlineMissed 10 5 12)
          desc (err/describe-failure-reason dm)]
      (assert (.contains desc "eadline") "should mention deadline"))))

;; --- Load limit edge cases ---

(run-test "four tasks across two workers: limits respected"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          t3 (simple-task "t3" "T3" 10 1)
          t4 (simple-task "t4" "T4" 10 1)
          w1 (limited-worker "w1" "W1" 2)
          w2 (limited-worker "w2" "W2" 2)
          result (s/schedule [t1 t2 t3 t4] [w1 w2] [])]
      (assert (ok? result))
      (assert (= 4 (count (.-assignments result))))
      (let [as (.-assignments result)
            w1-count (count (filter #(= "w1" (.-worker-id %)) as))
            w2-count (count (filter #(= "w2" (.-worker-id %)) as))]
        (assert (<= w1-count 2) "w1 should not exceed max-tasks=2")
        (assert (<= w2-count 2) "w2 should not exceed max-tasks=2")))))

;; --- Regression: existing scheduling behavior unchanged ---

(run-test "regression: single task schedules normally"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1] [w1] [])]
      (assert (ok? result))
      (assert (= 1 (count (.-assignments result)))))))

(run-test "regression: empty input returns ScheduleOk"
  (fn []
    (let [result (s/schedule [] [] [])]
      (assert (ok? result)))))

(run-test "regression: dependency chain still works"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (t/->Task "t2" "T2" 10 1 0 [] [] ["t1"] (t/->RetryPolicy 0 0))
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (ok? result))
      (let [as (.-assignments result)
            a1 (first (filter #(= "t1" (.-task-id %)) as))
            a2 (first (filter #(= "t2" (.-task-id %)) as))]
        (assert (= 0 (.-start-time a1)))
        (assert (= 10 (.-start-time a2)))))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
