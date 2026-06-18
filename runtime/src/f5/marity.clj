(ns f5.marity)

^{:line 3 :file "/tmp/fc/src/f5-marity.bclj"} (defrecord Pair [a b])

(defn pair-a [r] (:a r))

(defn pair-b [r] (:b r))

^{:line 4 :file "/tmp/fc/src/f5-marity.bclj"} (defn sum
  ([p]
    (let [match__0 p]
  (cond
    (instance? Pair match__0) (let [x (:a match__0) y (:b match__0)] (+ x y)))))
  ([m n]
    (let [t (+ m n)]
  t)))

^{:line 11 :file "/tmp/fc/src/f5-marity.bclj"} (defn use-it [^Pair p]
  ^{:line 12 :file "/tmp/fc/src/f5-marity.bclj"} (sum ^{:line 12 :file "/tmp/fc/src/f5-marity.bclj"} (pair-a p) ^{:line 12 :file "/tmp/fc/src/f5-marity.bclj"} (sum p)))
