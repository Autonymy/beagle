#!/usr/bin/env bb
;; Feature E: Exclusive Resources — Hidden Tests
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

(defn task-with-resources [id name duration priority resources]
  (t/->Task id name duration priority 0 [] resources [] (t/->RetryPolicy 0 0)))

(defn simple-worker [id name]
  (t/->Worker id name [] []))

(defn shared-resource [id name capacity]
  (t/->Resource id name capacity false))

(defn exclusive-resource [id name]
  (t/->Resource id name 1 true))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn failures [result]
  (if (ok? result)
    []
    (.-failures result)))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Hidden Tests ───────────────────────────────────────────────────────

(println "=== Feature E: Exclusive Resources — Hidden Tests ===")
(println "")

;; --- Match completeness: errors.bgl handles ResourceLocked ---

(run-test "describe-failure-reason handles ResourceLocked"
  (fn []
    (let [rl (t/->ResourceLocked "r1" "t1")
          desc (err/describe-failure-reason rl)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

(run-test "failure-reason-category handles ResourceLocked"
  (fn []
    (let [rl (t/->ResourceLocked "r1" "t1")
          cat (err/failure-reason-category rl)]
      (assert (= "resource-lock" cat) "ResourceLocked category should be 'resource-lock'"))))

(run-test "failure-is-structural? handles ResourceLocked"
  (fn []
    (let [rl (t/->ResourceLocked "r1" "t1")
          structural (err/failure-is-structural? rl)]
      (assert (= false structural) "ResourceLocked is not structural"))))

;; --- Error aggregation works with ResourceLocked ---

(run-test "group-failures-by-reason includes resource-lock category"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->ResourceLocked "r1" "t1"))
          grouped (err/group-failures-by-reason [sf])]
      (assert (contains? grouped "resource-lock") "should have 'resource-lock' category")
      (assert (= 1 (count (get grouped "resource-lock")))))))

(run-test "summarize-failures includes resource-lock failures"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->ResourceLocked "r1" "t1"))
          summary (err/summarize-failures [sf])]
      (assert (string? summary))
      (assert (.contains summary "resource-lock") "summary should mention resource-lock"))))

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

;; --- Exclusive resource edge cases ---

(run-test "mix of exclusive and shared resources"
  (fn []
    (let [;; t1 and t2 both need r1 (shared, cap=2) and r2 (exclusive)
          t1 (task-with-resources "t1" "T1" 10 1 ["r1" "r2"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r1" "r2"])
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (shared-resource "r1" "R1" 2)
          r2 (exclusive-resource "r2" "R2")
          result (s/schedule [t1 t2] [w1 w2] [r1 r2])]
      ;; r1 has capacity for both, but r2 is exclusive — t2 fails
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should fail")
        (assert (instance? scheduler.types.ResourceLocked (.-reason f2)))
        (assert (= "r2" (.-resource-id (.-reason f2))))))))

(run-test "three tasks: second locks, third also blocked"
  (fn []
    (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
          t3 (simple-task "t3" "T3" 10 1)
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (exclusive-resource "r1" "R1")
          result (s/schedule [t1 t2 t3] [w1 w2] [r1])]
      ;; t1 locks r1, t2 fails (needs r1), t3 succeeds (no resource needed)
      (assert (not (ok? result)))
      (assert (some? (failure-for result "t2")) "t2 should fail")
      (assert (nil? (failure-for result "t3")) "t3 should succeed (no resource needed)"))))

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

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
