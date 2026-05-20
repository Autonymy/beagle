#!/usr/bin/env bb
;; Feature B: Resource Maintenance Windows — Hidden Tests
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

(defn window [start end]
  (t/->Window start end))

(defn resource [id name capacity]
  (t/->Resource id name capacity []))

(defn resource-with-maintenance [id name capacity windows]
  (t/->Resource id name capacity windows))

(defn task-with-resource [id name duration priority resource-id]
  (t/->Task id name duration priority 0 [] [resource-id] [] (t/->RetryPolicy 0 0)))

(defn ok? [result]
  (instance? scheduler.types.ScheduleOk result))

(defn assignments [result]
  (if (ok? result)
    (.-assignments result)
    []))

(defn assignment-for [result task-id]
  (first (filter #(= task-id (.-task-id %)) (assignments result))))

;; ── Hidden Tests ───────────────────────────────────────────────────────

(println "=== Feature B: Resource Maintenance Windows — Hidden Tests ===")
(println "")

;; --- Match completeness: errors.bgl handles ResourceMaintenance ---

(run-test "describe-failure-reason handles ResourceMaintenance"
  (fn []
    (let [rm (t/->ResourceMaintenance "r1" (window 10 20))
          desc (err/describe-failure-reason rm)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

(run-test "failure-reason-category handles ResourceMaintenance"
  (fn []
    (let [rm (t/->ResourceMaintenance "r1" (window 10 20))
          cat (err/failure-reason-category rm)]
      (assert (= "maintenance" cat)
              (str "expected 'maintenance', got '" cat "'")))))

(run-test "failure-is-structural? handles ResourceMaintenance"
  (fn []
    (let [rm (t/->ResourceMaintenance "r1" (window 10 20))
          structural (err/failure-is-structural? rm)]
      (assert (= false structural)
              "ResourceMaintenance is not structural"))))

;; --- Match completeness: validator.bgl handles ResourceInMaintenance ---

(run-test "violations-by-kind categorizes ResourceInMaintenance"
  (fn []
    (let [a (t/->Assignment "t1" "w1" 10 20 1)
          violation (t/->Violation a (t/->ResourceInMaintenance "r1" (window 10 20)))
          grouped (v/violations-by-kind [violation])]
      (assert (contains? grouped "resource-maintenance")
              (str "should have 'resource-maintenance' key, got keys: " (keys grouped))))))

(run-test "describe-violation handles ResourceInMaintenance"
  (fn []
    (let [a (t/->Assignment "t1" "w1" 10 20 1)
          violation (t/->Violation a (t/->ResourceInMaintenance "r1" (window 10 20)))
          desc (v/describe-violation violation)]
      (assert (string? desc) "should return a string")
      (assert (pos? (count desc)) "should not be empty"))))

;; --- Validator catches maintenance violations ---

(run-test "validator catches assignment during resource maintenance"
  (fn []
    (let [;; Assignment at [10, 20), resource in maintenance [5, 25)
          a (t/->Assignment "t1" "w1" 10 20 1)
          t1 (task-with-resource "t1" "T1" 10 1 "r1")
          w1 (simple-worker "w1" "W1")
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 5 25)])
          violations (v/validate-assignments [a] [t1] [w1] [r1])]
      (assert (pos? (count violations))
              "should detect maintenance violation"))))

(run-test "validator passes when assignment does not overlap maintenance"
  (fn []
    (let [;; Assignment at [0, 5), maintenance at [10, 20) — no overlap
          a (t/->Assignment "t1" "w1" 0 5 1)
          t1 (task-with-resource "t1" "T1" 5 1 "r1")
          w1 (simple-worker "w1" "W1")
          r1 (resource-with-maintenance "r1" "R1" 1 [(window 10 20)])
          violations (v/validate-assignments [a] [t1] [w1] [r1])]
      (assert (zero? (count violations))
              "should not detect violation"))))

;; --- Error aggregation works with ResourceMaintenance ---

(run-test "group-failures-by-reason includes maintenance category"
  (fn []
    (let [sf (t/->ScheduleFailure "t1" (t/->ResourceMaintenance "r1" (window 10 20)))
          grouped (err/group-failures-by-reason [sf])]
      (assert (contains? grouped "maintenance")
              "should have 'maintenance' category"))))

;; --- Regression: existing behavior unchanged ---

(run-test "regression: resource capacity still works"
  (fn []
    (let [t1 (task-with-resource "t1" "T1" 10 1 "r1")
          t2 (task-with-resource "t2" "T2" 10 2 "r1")
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (resource "r1" "R1" 1)
          result (s/schedule [t1 t2] [w1 w2] [r1])]
      (assert (ok? result))
      (let [a1 (assignment-for result "t1")
            a2 (assignment-for result "t2")]
        ;; capacity=1, so second task waits
        (assert (>= (.-start-time a2) (.-end-time a1))
                "capacity=1 should serialize")))))

(run-test "regression: resource with empty maintenance equals old behavior"
  (fn []
    (let [t1 (task-with-resource "t1" "T1" 10 1 "r1")
          w1 (simple-worker "w1" "W1")
          r1 (resource "r1" "R1" 1)
          result (s/schedule [t1] [w1] [r1])]
      (assert (ok? result))
      (let [a (assignment-for result "t1")]
        (assert (= 0 (.-start-time a)))))))

(run-test "regression: describe-failure-reason still handles DeadlineMissed"
  (fn []
    (let [dm (t/->DeadlineMissed 10 5 12)
          desc (err/describe-failure-reason dm)]
      (assert (.contains desc "eadline")))))

(run-test "regression: multiple resources still checked"
  (fn []
    (let [t1 (t/->Task "t1" "T1" 10 1 0 [] ["r1" "r2"] [] (t/->RetryPolicy 0 0))
          t2 (t/->Task "t2" "T2" 10 2 0 [] ["r1"] [] (t/->RetryPolicy 0 0))
          w1 (simple-worker "w1" "W1")
          w2 (simple-worker "w2" "W2")
          r1 (resource "r1" "R1" 1)
          r2 (resource "r2" "R2" 1)
          result (s/schedule [t1 t2] [w1 w2] [r1 r2])]
      (assert (ok? result))
      (let [a2 (assignment-for result "t2")]
        ;; r1 has capacity=1, t1 uses r1 [0,10), t2 must wait
        (assert (>= (.-start-time a2) 10))))))

;; ── Summary ────────────────────────────────────────────────────────────

(println "")
(let [results @*results*
      passed (count (filter true? results))
      failed (count (filter false? results))
      total (count results)]
  (println (str "Results: " passed "/" total " passed, " failed " failed"))
  (when (pos? failed)
    (System/exit 1)))
