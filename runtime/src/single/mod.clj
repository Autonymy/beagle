(ns single.mod)

^{:line 5 :file "/tmp/sweep5/renamed-single.bclj"} (defrecord Pt [x y])

(defn pt-x [r] (:x r))

(defn pt-y [r] (:y r))

^{:line 7 :file "/tmp/sweep5/renamed-single.bclj"} (defn ^Pt origin []
  ^{:line 7 :file "/tmp/sweep5/renamed-single.bclj"} (->Point 0 0))
