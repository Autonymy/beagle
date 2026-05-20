#!/usr/bin/env bb
;; Feature C: Worker Load Limits — Visible Tests
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

;; Helpers — workers with max-tasks field (last positional arg)
(defn simple-task [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0)))

(defn simple-worker [id name]
  (t/->Worker id name [] [] 0))

(defn limited-worker [id name max-tasks]
  (t/->Worker id name [] [] max-tasks))

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

(println "=== Feature C: Worker Load Limits — Visible Tests ===")
(println "")

(run-test "unlimited workers schedule normally"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          t3 (simple-task "t3" "T3" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2 t3] [w1] [])]
      (assert (ok? result))
      (assert (= 3 (count (assignments result)))))))

(run-test "worker with max-tasks=1 sends overflow to second worker"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          w1 (limited-worker "w1" "W1" 1)
          w2 (limited-worker "w2" "W2" 1)
          result (s/schedule [t1 t2] [w1 w2] [])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result))))
      (let [as (assignments result)
            w1-tasks (filter #(= "w1" (.-worker-id %)) as)
            w2-tasks (filter #(= "w2" (.-worker-id %)) as)]
        (assert (= 1 (count w1-tasks)))
        (assert (= 1 (count w2-tasks)))))))

(run-test "only worker at limit fails remaining tasks with WorkerOverloaded"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          w1 (limited-worker "w1" "W1" 1)
          result (s/schedule [t1 t2] [w1] [])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should have a failure")
        (assert (instance? scheduler.types.WorkerOverloaded (.-reason f2))
                "t2 should fail with WorkerOverloaded")
        (assert (= "w1" (.-worker-id (.-reason f2))))
        (assert (= 1 (.-max-tasks (.-reason f2))))))))

(run-test "workers with different limits distribute correctly"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          t3 (simple-task "t3" "T3" 10 1)
          w1 (limited-worker "w1" "W1" 2)
          w2 (limited-worker "w2" "W2" 1)
          result (s/schedule [t1 t2 t3] [w1 w2] [])]
      (assert (ok? result))
      (assert (= 3 (count (assignments result)))))))

(run-test "max-tasks=0 means unlimited"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          t3 (simple-task "t3" "T3" 10 1)
          t4 (simple-task "t4" "T4" 10 1)
          w1 (simple-worker "w1" "W1")
          result (s/schedule [t1 t2 t3 t4] [w1] [])]
      (assert (ok? result))
      (assert (= 4 (count (assignments result)))))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
