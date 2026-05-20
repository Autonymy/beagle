#!/usr/bin/env bb
;; Feature B: Resource Maintenance Windows — Visible Tests
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

;; Helpers
(defn simple-task [id name duration priority]
  (t/->Task id name duration priority 0 [] [] [] (t/->RetryPolicy 0 0)))

(defn task-with-resource [id name duration priority resource-id]
  (t/->Task id name duration priority 0 [] [resource-id] [] (t/->RetryPolicy 0 0)))

(defn task-with-resource-deadline [id name duration priority resource-id deadline]
  (t/->Task id name duration priority deadline [] [resource-id] [] (t/->RetryPolicy 0 0)))

(defn simple-worker [id name]
  (t/->Worker id name [] []))

(defn window [start end]
  (t/->Window start end))

;; Resource with maintenance windows (new constructor)
(defn resource-with-maintenance [id name capacity windows]
  (t/->Resource id name capacity windows))

;; Resource without maintenance (backwards compatible)
(defn resource [id name capacity]
  (t/->Resource id name capacity []))

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

(defn assignment-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (assignments result))))

(defn failure-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (failures result))))

;; ── Tests ──────────────────────────────────────────────────────────────

(println "=== Feature B: Resource Maintenance Windows — Visible Tests ===")
(println "")

(run-test "resource with no maintenance windows works normally"
  (fn []
    (let [t1 (task-with-resource "t1" "T1" 10 1 "r1")
          w1 (simple-worker "w1" "W1")
          r1 (resource "r1" "R1" 1)
          result (s/schedule [t1] [w1] [r1])]
      (assert (ok? result))
      (let [a (assignment-for result "t1")]
        (assert (= 0 (.-start-time a)))))))

(run-test "task slides past resource maintenance window"
  (fn []
    (let [t1 (task-with-resource "t1" "T1" 10 1 "r1")
          w1 (simple-worker "w1" "W1")
          ;; Resource in maintenance [0, 20)
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 0 20)])
          result (s/schedule [t1] [w1] [r1])]
      (assert (ok? result))
      (let [a (assignment-for result "t1")]
        ;; Task should start at 20 (after maintenance ends)
        (assert (= 20 (.-start-time a))
                (str "expected start=20, got " (.-start-time a)))))))

(run-test "task fits before maintenance window"
  (fn []
    (let [;; Task duration=5, maintenance starts at 10
          t1 (task-with-resource "t1" "T1" 5 1 "r1")
          w1 (simple-worker "w1" "W1")
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 10 30)])
          result (s/schedule [t1] [w1] [r1])]
      (assert (ok? result))
      (let [a (assignment-for result "t1")]
        ;; Task fits [0, 5) before maintenance [10, 30)
        (assert (= 0 (.-start-time a)))))))

(run-test "task cannot meet deadline due to maintenance"
  (fn []
    (let [;; Task needs 10 min, maintenance [0, 20), deadline 25
          ;; After maintenance, earliest start=20, end=30 > deadline=25
          t1 (task-with-resource-deadline "t1" "T1" 10 1 "r1" 25)
          w1 (simple-worker "w1" "W1")
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 0 20)])
          result (s/schedule [t1] [w1] [r1])]
      (assert (not (ok? result)) "should fail — can't meet deadline"))))

(run-test "two tasks share resource: second slides past maintenance"
  (fn []
    (let [t1 (task-with-resource "t1" "T1" 10 1 "r1")
          t2 (task-with-resource "t2" "T2" 10 2 "r1")
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          ;; capacity=1, maintenance [15, 25)
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 15 25)])
          result (s/schedule [t1 t2] [w1 w2] [r1])]
      (assert (ok? result))
      (let [a1 (assignment-for result "t1")
            a2 (assignment-for result "t2")]
        ;; t1 starts at 0, ends at 10 (before maintenance)
        (assert (= 0 (.-start-time a1)))
        ;; t2: can't start at 10 (would end at 20, overlaps [15,25))
        ;; Must wait until 25
        (assert (>= (.-start-time a2) 25)
                (str "t2 should start at or after 25, got " (.-start-time a2)))))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
