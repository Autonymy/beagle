(ns scheduler.types)

^{:line 18 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Window [start end])

(defn window-start [r] (:start r))

(defn window-end [r] (:end r))

^{:line 23 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord RetryPolicy [max-attempts backoff])

(defn retrypolicy-max-attempts [r] (:max-attempts r))

(defn retrypolicy-backoff [r] (:backoff r))

^{:line 30 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Task [id name duration priority deadline capabilities resources depends-on retry-policy])

(defn task-id [r] (:id r))

(defn task-name [r] (:name r))

(defn task-duration [r] (:duration r))

(defn task-priority [r] (:priority r))

(defn task-deadline [r] (:deadline r))

(defn task-capabilities [r] (:capabilities r))

(defn task-resources [r] (:resources r))

(defn task-depends-on [r] (:depends-on r))

(defn task-retry-policy [r] (:retry-policy r))

^{:line 41 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Worker [id name capabilities unavailable])

(defn worker-id [r] (:id r))

(defn worker-name [r] (:name r))

(defn worker-capabilities [r] (:capabilities r))

(defn worker-unavailable [r] (:unavailable r))

^{:line 47 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Resource [id name capacity])

(defn resource-id [r] (:id r))

(defn resource-name [r] (:name r))

(defn resource-capacity [r] (:capacity r))

^{:line 52 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Dependency [from-task to-task])

(defn dependency-from-task [r] (:from-task r))

(defn dependency-to-task [r] (:to-task r))

^{:line 59 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Assignment [task-id worker-id start-time end-time attempt])

(defn assignment-task-id [r] (:task-id r))

(defn assignment-worker-id [r] (:worker-id r))

(defn assignment-start-time [r] (:start-time r))

(defn assignment-end-time [r] (:end-time r))

(defn assignment-attempt [r] (:attempt r))

^{:line 69 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord DependencyCycle [cycle])

(defn dependencycycle-cycle [r] (:cycle r))

^{:line 71 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord NoCapableWorker [required available])

(defn nocapableworker-required [r] (:required r))

(defn nocapableworker-available [r] (:available r))

^{:line 74 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord ResourceOverCapacity [resource-id capacity demanded])

(defn resourceovercapacity-resource-id [r] (:resource-id r))

(defn resourceovercapacity-capacity [r] (:capacity r))

(defn resourceovercapacity-demanded [r] (:demanded r))

^{:line 78 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord DeadlineMissed [earliest-start duration deadline])

(defn deadlinemissed-earliest-start [r] (:earliest-start r))

(defn deadlinemissed-duration [r] (:duration r))

(defn deadlinemissed-deadline [r] (:deadline r))

^{:line 82 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord WorkerUnavailable [worker-id windows])

(defn workerunavailable-worker-id [r] (:worker-id r))

(defn workerunavailable-windows [r] (:windows r))

;; FailureReason = DependencyCycle | NoCapableWorker | ResourceOverCapacity | DeadlineMissed | WorkerUnavailable

^{:line 96 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord ScheduleFailure [task-id reason])

(defn schedulefailure-task-id [r] (:task-id r))

(defn schedulefailure-reason [r] (:reason r))

^{:line 103 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord ScheduleOk [assignments])

(defn scheduleok-assignments [r] (:assignments r))

^{:line 105 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord ScheduleError [failures])

(defn scheduleerror-failures [r] (:failures r))

;; ScheduleResult = ScheduleOk | ScheduleError

^{:line 117 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord DependencyGraph [adjacency reverse-adj task-ids])

(defn dependencygraph-adjacency [r] (:adjacency r))

(defn dependencygraph-reverse-adj [r] (:reverse-adj r))

(defn dependencygraph-task-ids [r] (:task-ids r))

^{:line 125 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord CapabilityMismatch [missing])

(defn capabilitymismatch-missing [r] (:missing r))

^{:line 127 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord WindowConflict [window])

(defn windowconflict-window [r] (:window r))

^{:line 129 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord ResourceExceeded [resource-id at-time count capacity])

(defn resourceexceeded-resource-id [r] (:resource-id r))

(defn resourceexceeded-at-time [r] (:at-time r))

(defn resourceexceeded-count [r] (:count r))

(defn resourceexceeded-capacity [r] (:capacity r))

^{:line 134 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord DependencyOrder [dependency-id dep-end task-start])

(defn dependencyorder-dependency-id [r] (:dependency-id r))

(defn dependencyorder-dep-end [r] (:dep-end r))

(defn dependencyorder-task-start [r] (:task-start r))

^{:line 138 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord DeadlineExceeded [deadline end-time])

(defn deadlineexceeded-deadline [r] (:deadline r))

(defn deadlineexceeded-end-time [r] (:end-time r))

^{:line 141 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord OverlapViolation [other-task-id])

(defn overlapviolation-other-task-id [r] (:other-task-id r))

;; ViolationKind = CapabilityMismatch | WindowConflict | ResourceExceeded | DependencyOrder | DeadlineExceeded | OverlapViolation

^{:line 151 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defrecord Violation [assignment kind])

(defn violation-assignment [r] (:assignment r))

(defn violation-kind [r] (:kind r))

^{:line 159 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (def max-slide-window 1440)

^{:line 162 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (def no-retry ^{:line 162 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->RetryPolicy 0 0))

^{:line 168 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-task [id name duration priority deadline capabilities resources depends-on retry-policy]
  ^{:line 177 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Task id name duration priority deadline capabilities resources depends-on retry-policy))

^{:line 180 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-simple-task [id name duration priority]
  ^{:line 184 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Task id name duration priority 0 ^{:line 184 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} [] ^{:line 184 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} [] ^{:line 184 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} [] ^{:line 184 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->RetryPolicy 0 0)))

^{:line 186 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-worker [id name capabilities unavailable]
  ^{:line 190 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Worker id name capabilities unavailable))

^{:line 192 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-resource [id name capacity]
  ^{:line 195 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Resource id name capacity))

^{:line 197 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-window [start end]
  ^{:line 198 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Window start end))

^{:line 200 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (defn make-assignment [task-id worker-id start-time end-time attempt]
  ^{:line 205 :file "/home/tom/code/beagle/experiments/e16-workflow-scheduler/golden/beagle/types.rkt"} (->Assignment task-id worker-id start-time end-time attempt))
