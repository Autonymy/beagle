(ns c8.h)

^{:line 3 :file "/tmp/cg/c8_hof_threading/h.bclj"} (defn inc1 [x]
  ^{:line 3 :file "/tmp/cg/c8_hof_threading/h.bclj"} (+ x 1))

^{:line 4 :file "/tmp/cg/c8_hof_threading/h.bclj"} (defn dbl [x]
  ^{:line 4 :file "/tmp/cg/c8_hof_threading/h.bclj"} (* x 2))

^{:line 6 :file "/tmp/cg/c8_hof_threading/h.bclj"} (defn maps [xs]
  ^{:line 6 :file "/tmp/cg/c8_hof_threading/h.bclj"} (mapv inc1 xs))

^{:line 8 :file "/tmp/cg/c8_hof_threading/h.bclj"} (defn threaded [x]
  ^{:line 8 :file "/tmp/cg/c8_hof_threading/h.bclj"} (-> x dbl inc1))

^{:line 10 :file "/tmp/cg/c8_hof_threading/h.bclj"} (defn lam [xs]
  ^{:line 10 :file "/tmp/cg/c8_hof_threading/h.bclj"} (mapv ^{:line 10 :file "/tmp/cg/c8_hof_threading/h.bclj"} (fn [y] ^{:line 10 :file "/tmp/cg/c8_hof_threading/h.bclj"} (inc1 y)) xs))
