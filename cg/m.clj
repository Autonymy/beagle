(ns cg.m)

^{:line 4 :file "/tmp/cg4/m.bclj"} (defprotocol Render
  (render [self]))

^{:line 7 :file "/tmp/cg4/m.bclj"} (defrecord Box [w])

(defn box-w [r] (:w r))

^{:line 9 :file "/tmp/cg4/m.bclj"} (defn ^String wrap [n]
  ^{:line 10 :file "/tmp/cg4/m.bclj"} (str "<" n ">"))

^{:line 13 :file "/tmp/cg4/m.bclj"} (extend-type Box
  Render
  (render [self]
    ^{:line 15 :file "/tmp/cg4/m.bclj"} (wrap ^{:line 15 :file "/tmp/cg4/m.bclj"} (box-w self))))

^{:line 18 :file "/tmp/cg4/m.bclj"} (defn ^String direct [n]
  ^{:line 18 :file "/tmp/cg4/m.bclj"} (wrap n))

^{:line 20 :file "/tmp/cg4/m.bclj"} (defn ^String show [^Box b]
  ^{:line 21 :file "/tmp/cg4/m.bclj"} (render b))
