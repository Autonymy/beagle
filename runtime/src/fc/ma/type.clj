(ns fc.ma-type)

^{:line 3 :file "/tmp/triage/matype.bclj"} (defrecord Box [v])

(defn box-v [r] (:v r))

^{:line 4 :file "/tmp/triage/matype.bclj"} (defn wrap
  ([b]
    b)
  ([b c]
    b))
