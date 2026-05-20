#!/usr/bin/env bb
;; Feature D: Cost Budgets — Hidden Tests
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
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) 0))

(defn costed-task [id name duration priority cost]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) cost))

(defn simple-worker [id name]
  (t/->Worker id name [] []))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn failures [result]
  (if (ok? result)
    []
    (.-failures result)))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Hidden Tests ───────────────────────────────────────────────────────

(println "=== Feature D: Cost Budgets — Hidden Tests ===")
(println "")

;; --- Match completeness: errors.bgl handles BudgetExceeded ---

(run-test "describe-failure-reason handles BudgetExceeded"
  (fn []
    (let [be (t/->BudgetExceeded 50 980 1000)
          desc (err/describe-failure-reason be)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

(run-test "failure-reason-category handles BudgetExceeded"
  (fn []
    (let [be (t/->BudgetExceeded 50 980 1000)
          cat (err/failure-reason-category be)]
      (assert (= "budget" cat) "BudgetExceeded category should be 'budget'"))))

(run-test "failure-is-structural? handles BudgetExceeded"
  (fn []
    (let [be (t/->BudgetExceeded 50 980 1000)
          structural (err/failure-is-structural? be)]
      (assert (= false structural) "BudgetExceeded is not structural"))))

;; --- Error aggregation works with BudgetExceeded ---

(run-test "group-failures-by-reason includes budget category"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->BudgetExceeded 500 600 1000))
          grouped (err/group-failures-by-reason [sf])]
      (assert (contains? grouped "budget") "should have 'budget' category")
      (assert (= 1 (count (get grouped "budget")))))))

(run-test "summarize-failures includes budget failures"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->BudgetExceeded 500 600 1000))
          summary (err/summarize-failures [sf])]
      (assert (string? summary))
      (assert (.contains summary "budget") "summary should mention budget"))))

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

;; --- Budget edge cases ---

(run-test "budget exactly at limit succeeds"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 500)
          t2 (costed-task "t2" "T2" 10 1 500)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      ;; 500 + 500 = 1000 exactly at limit — should succeed
      (assert (ok? result))
      (assert (= 2 (count (.-assignments result)))))))

(run-test "multiple tasks: budget runs out mid-schedule"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 400)
          t2 (costed-task "t2" "T2" 10 1 400)
          t3 (costed-task "t3" "T3" 10 1 400)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2 t3] [w1] [])]
      ;; 400 + 400 = 800 OK, 400 + 800 = 1200 > 1000 — t3 fails
      (assert (not (ok? result)))
      (assert (nil? (failure-for result "t1")) "t1 should succeed")
      (assert (nil? (failure-for result "t2")) "t2 should succeed")
      (let [f3 (failure-for result "t3")]
        (assert (some? f3) "t3 should fail")
        (assert (instance? scheduler.types.BudgetExceeded (.-reason f3)))))))

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
