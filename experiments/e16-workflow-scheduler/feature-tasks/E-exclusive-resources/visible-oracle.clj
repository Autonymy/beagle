#!/usr/bin/env bb
;; Feature E: Exclusive Resources — Visible Tests
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

;; Helpers — resources with exclusive field (last positional arg)
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

(println "=== Feature E: Exclusive Resources — Visible Tests ===")
(println "")

(run-test "non-exclusive resources work normally"
  (fn []
    (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (shared-resource "r1" "R1" 2)
          result (s/schedule [t1 t2] [w1 w2] [r1])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "exclusive resource: first task locks, second fails"
  (fn []
    (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (exclusive-resource "r1" "R1")
          result (s/schedule [t1 t2] [w1 w2] [r1])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should have a failure")
        (assert (instance? scheduler.types.ResourceLocked (.-reason f2))
                "t2 should fail with ResourceLocked")
        (assert (= "r1" (.-resource-id (.-reason f2))))
        (assert (= "t1" (.-locked-by (.-reason f2))))))))

(run-test "different exclusive resources: both tasks succeed"
  (fn []
    (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r2"])
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (exclusive-resource "r1" "R1")
          r2 (exclusive-resource "r2" "R2")
          result (s/schedule [t1 t2] [w1 w2] [r1 r2])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

(run-test "exclusive resource ignores capacity: high capacity still locks"
  (fn []
    (let [t1 (task-with-resources "t1" "T1" 10 1 ["r1"])
          t2 (task-with-resources "t2" "T2" 10 1 ["r1"])
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          ;; capacity=5 but exclusive=true — only one task allowed
          r1 (t/->Resource "r1" "R1" 5 true)
          result (s/schedule [t1 t2] [w1 w2] [r1])]
      (assert (not (ok? result)))
      (let [f2 (failure-for result "t2")]
        (assert (some? f2) "t2 should fail despite capacity=5")
        (assert (instance? scheduler.types.ResourceLocked (.-reason f2)))))))

(run-test "tasks not requiring any resource schedule normally"
  (fn []
    (let [t1 (simple-task "t1" "T1" 10 1)
          t2 (simple-task "t2" "T2" 10 1)
          w1 (simple-worker "w1" "W1")
          r1 (exclusive-resource "r1" "R1")
          result (s/schedule [t1 t2] [w1] [r1])]
      (assert (ok? result))
      (assert (= 2 (count (assignments result)))))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
