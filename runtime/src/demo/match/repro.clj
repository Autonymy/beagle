(ns demo.match-repro)

^{:line 7 :file "/tmp/triage/mf-renamed.bclj"} (defrecord Ok [value])

(defn ok-value [r] (:value r))

^{:line 9 :file "/tmp/triage/mf-renamed.bclj"} (defrecord Err [error])

(defn err-error [r] (:error r))

;; Result = Yes | Err

^{:line 13 :file "/tmp/triage/mf-renamed.bclj"} (defn ^String handle [r]
  ^{:line 13 :file "/tmp/triage/mf-renamed.bclj"} (let [match__0 r]
  (cond
    (instance? Yes match__0) "ok"
    (instance? Err match__0) (let [e (:error match__0)] e))))

^{:line 15 :file "/tmp/triage/mf-renamed.bclj"} (defn ^String handle2 [r]
  ^{:line 15 :file "/tmp/triage/mf-renamed.bclj"} (let [match__1 r]
  (cond
    (instance? Yes match__1) "yes"
    (instance? Err match__1) (let [e (:error match__1)] "no"))))
