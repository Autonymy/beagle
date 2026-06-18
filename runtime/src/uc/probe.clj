(ns uc.probe)

^{:line 3 :file "/tmp/fc/uc-probe.bclj"} (defrecord Bar [x])

(defn bar-x [r] (:x r))

^{:line 4 :file "/tmp/fc/uc-probe.bclj"} (defn getx [^Bar b]
  ^{:line 4 :file "/tmp/fc/uc-probe.bclj"} (bar-x b))
