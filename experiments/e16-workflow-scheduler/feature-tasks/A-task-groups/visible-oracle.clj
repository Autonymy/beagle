#!/usr/bin/env bb
;; Feature A: Task Groups — Visible Tests
;; The agent can run these to verify its implementation.

(require '[scheduler.scheduler :as s]
         '[scheduler.types :as t]
         '[scheduler.validator :as v])

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

;; Helpers — tasks with group field (last positional arg)
(defn simple-task [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) ""))

(defn grouped-task [id name duration priority group]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) group))

(defn task-with-deadline [id name duration priority deadline]
  (t/->Task id name duration priority deadline [] [] [] (t/->RetryPolicy 0 0) ""))

(defn grouped-task-with-deadline [id name duration priority deadline group]
  (t/->Task id name duration priority deadline [] [] [] (t/->RetryPolicy 0 0) group))

(defn simple-worker [id name]
  (t/->Worker id name [] []))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn assignments [result]
  (if (ok? result)
    (.-assignments result)
    []))

(defn failures [result]
  (if (ok? result)
    []
    (.-failures result)))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Tests ──────────────────────────────────────────────────────────────

(println "=== Feature A: Task Groups — Visible Tests ===")
(println "")

(run-test "ungrouped tasks schedule normally"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "grouped tasks all succeed when no failures"
  (fn []
    (let [t1 (grouped-task "t1" "T1" 10 1 "batch-1")
          t2 (grouped-task "t2" "T2" 10 1 "batch-1")
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          result (s/schedule [t1 t2] [w1 w2] [])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "group failure: one task fails, other in same group also fails"
  (fn []
    (let [;; t1 has impossible deadline (duration=100, deadline=5)
          t1 (grouped-task-with-deadline "t1" "T1" 100 1 5 "batch-1")
          ;; t2 is fine on its own, but in same group
          t2 (grouped-task "t2" "T2" 10 1 "batch-1")
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should have a failure")
        (assert (instance? scheduler.types.GroupFailure (.-reason f2))
                "t2 should fail with GroupFailure")
        (assert (= "batch-1" (.-group-id (.-reason f2))))
        (assert (= "t1" (.-failed-task-id (.-reason f2))))))))

(run-test "group failure does not affect different group"
  (fn []
    (let [;; batch-1: t1 fails
          t1 (grouped-task-with-deadline "t1" "T1" 100 1 5 "batch-1")
          ;; batch-2: t2 should succeed independently
          t2 (grouped-task "t2" "T2" 10 1 "batch-2")
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      ;; t1 fails, but t2 is in a different group — should still succeed
      ;; Result should be ScheduleError because t1 failed
      (assert (not (ok? result)))
      (let [fs (failures result)
            f2 (failure-for result "t2")]
        ;; t2 should NOT have a GroupFailure — it's in a different group
        (assert (nil? f2) "t2 should not fail — different group")))))

(run-test "ungrouped task unaffected by group failure"
  (fn []
    (let [t1 (grouped-task-with-deadline "t1" "T1" 100 1 5 "batch-1")
          ;; t2 is ungrouped
          t2 (simple-task "t2" "T2" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (nil? f2) "ungrouped t2 should not be affected by batch-1 failure")))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
