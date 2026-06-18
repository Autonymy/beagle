(ns cg.impl
  (:require [cg.proto :as p :refer [area perim]]))

^{:line 5 :file "/tmp/cg3/impl.bclj"} (defrecord Square [side])

(defn square-side [r] (:side r))

^{:line 7 :file "/tmp/cg3/impl.bclj"} (defn helper [x]
  ^{:line 7 :file "/tmp/cg3/impl.bclj"} (* x x))

^{:line 10 :file "/tmp/cg3/impl.bclj"} (extend-type Square
  Shape
  (area [self]
    ^{:line 12 :file "/tmp/cg3/impl.bclj"} (helper ^{:line 12 :file "/tmp/cg3/impl.bclj"} (square-side self)))
  (perim [self]
    ^{:line 13 :file "/tmp/cg3/impl.bclj"} (* 4 ^{:line 13 :file "/tmp/cg3/impl.bclj"} (square-side self))))

^{:line 16 :file "/tmp/cg3/impl.bclj"} (defn total [^Square s]
  ^{:line 17 :file "/tmp/cg3/impl.bclj"} (+ ^{:line 17 :file "/tmp/cg3/impl.bclj"} (area s) ^{:line 17 :file "/tmp/cg3/impl.bclj"} (p/describe 5)))
