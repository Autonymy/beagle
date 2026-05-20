#!/usr/bin/env bb
;; Feature D: Cost Budgets — Visible Tests
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

;; Helpers — tasks with cost field (last positional arg)
(defn simple-task [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) 0))

(defn costed-task [id name duration priority cost]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0) cost))

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

(println "=== Feature D: Cost Budgets — Visible Tests ===")
(println "")

(run-test "free tasks (cost=0) schedule normally"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "tasks within budget all succeed"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 400)
          t2 (costed-task "t2" "T2" 10 1 400)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "task exceeding budget fails with BudgetExceeded"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 600)
          t2 (costed-task "t2" "T2" 10 1 500)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2] [w1] [])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should have a failure")
        (assert (instance? scheduler.types.BudgetExceeded (.-reason f2))
                "t2 should fail with BudgetExceeded")
        (assert (= 500 (.-task-cost (.-reason f2))))
        (assert (= 600 (.-total-spent (.-reason f2))))
        (assert (= 1000 (.-budget (.-reason f2))))))))

(run-test "earlier tasks within budget succeed, later task exceeds"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 300)
          t2 (costed-task "t2" "T2" 10 1 300)
          t3 (costed-task "t3" "T3" 10 1 300)
          t4 (costed-task "t4" "T4" 10 1 300)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2 t3 t4] [w1] [])]
      (assert (not (ok? result)))
      ;; t1, t2, t3 should succeed (300+300+300=900 <= 1000)
      ;; t4 would bring total to 1200 > 1000 — should fail
      (let [fs (failures result)]
        (assert (= 1 (count fs)) "only t4 should fail")
        (let [f4 (failure-for result "t4")]
          (assert (some? f4) "t4 should fail")
          (assert (instance? scheduler.types.BudgetExceeded (.-reason f4))))))))

(run-test "mix of free and costed tasks: free tasks don't count toward budget"
  (fn []
    (let [t1 (costed-task "t1" "T1" 10 1 900)
          t2 (simple-task "t2" "T2" 10 1)
          t3 (costed-task "t3" "T3" 10 1 200)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2 t3] [w1] [])]
      ;; t1 costs 900, t2 is free (doesn't count), t3 costs 200
      ;; 900 + 200 = 1100 > 1000 — t3 should fail
      (assert (not (ok? result)))
      (assert (nil? (failure-for result "t2")) "free task t2 should succeed")
      (assert (some? (failure-for result "t3")) "t3 should fail on budget"))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
