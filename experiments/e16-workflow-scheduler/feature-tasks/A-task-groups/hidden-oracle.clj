#!/usr/bin/env bb
;; Feature A: Task Groups — Hidden Tests
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

(defn simple-worker [id name]
  (t/->Worker id name [] []))

(defn grouped-task-with-deadline [id name duration priority deadline group]
  (t/->Task id name duration priority deadline [] [] [] (t/->RetryPolicy 0 0) group))

(defn grouped-task [id name duration priority group]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) group))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn failures [result]
  (if (ok? result)
    []
    (.-failures result)))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Hidden Tests ───────────────────────────────────────────────────────

(println "=== Feature A: Task Groups — Hidden Tests ===")
(println "")

;; --- Match completeness: errors.bgl handles GroupFailure ---

(run-test "describe-failure-reason handles GroupFailure"
  (fn []
    (let [gf (t/->GroupFailure "batch-1" "t1")
          desc (err/describe-failure-reason gf)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

(run-test "failure-reason-category handles GroupFailure"
  (fn []
    (let [gf (t/->GroupFailure "batch-1" "t1")
          cat (err/failure-reason-category gf)]
      (assert (= "group" cat) "GroupFailure category should be 'group'"))))

(run-test "failure-is-structural? handles GroupFailure"
  (fn []
    (let [gf (t/->GroupFailure "batch-1" "t1")
          structural (err/failure-is-structural? gf)]
      (assert (= false structural) "GroupFailure is not structural"))))

;; --- Error aggregation works with GroupFailure ---

(run-test "group-failures-by-reason includes group category"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->GroupFailure "batch-1" "t1"))
          grouped (err/group-failures-by-reason [sf])]
      (assert (contains? grouped "group") "should have 'group' category")
      (assert (= 1 (count (get grouped "group")))))))

(run-test "summarize-failures includes group failures"
  (fn []
    (let [sf (t/->ScheduleFailure "t2" (t/->GroupFailure "batch-1" "t1"))
          summary (err/summarize-failures [sf])]
      (assert (string? summary))
      (assert (.contains summary "group") "summary should mention group"))))

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

;; --- Multi-group interaction ---

(run-test "three groups: only the failed group cascades"
  (fn []
    (let [;; batch-1: t1 fails (deadline), t2 should get GroupFailure
          t1 (grouped-task-with-deadline "t1" "T1" 100 1 5 "batch-1")
          t2 (grouped-task "t2" "T2" 10 1 "batch-1")
          ;; batch-2: both succeed
          t3 (grouped-task "t3" "T3" 10 2 "batch-2")
          t4 (grouped-task "t4" "T4" 10 2 "batch-2")
          ;; batch-3: t5 fails (deadline), t6 should get GroupFailure
          t5 (grouped-task-with-deadline "t5" "T5" 100 3 5 "batch-3")
          t6 (grouped-task "t6" "T6" 10 3 "batch-3")
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          result (s/schedule [t1 t2 t3 t4 t5 t6] [w1 w2] [])]
      ;; Result is ScheduleError because some tasks failed
      (assert (not (ok? result)))
      (let [fs (failures result)]
        ;; t2 should be GroupFailure from batch-1
        (let [f2 (failure-for result "t2")]
          (assert (some? f2) "t2 should fail")
          (assert (instance? scheduler.types.GroupFailure (.-reason f2))))
        ;; t6 should be GroupFailure from batch-3
        (let [f6 (failure-for result "t6")]
          (assert (some? f6) "t6 should fail")
          (assert (instance? scheduler.types.GroupFailure (.-reason f6))))
        ;; t3 and t4 should NOT have failures (batch-2 was fine)
        (assert (nil? (failure-for result "t3")) "t3 should not fail")
        (assert (nil? (failure-for result "t4")) "t4 should not fail")))))

;; --- Regression: existing scheduling behavior unchanged ---

(run-test "regression: single task schedules normally"
  (fn []
    (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 0 0) "")
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
    (let [t1 (t/->Task "t1" "T1" 10 1 0 [] [] [] (t/->RetryPolicy 0 0) "")
          t2 (t/->Task "t2" "T2" 10 1 0 [] [] ["t1"] (t/->RetryPolicy 0 0) "")
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
