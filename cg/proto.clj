(ns cg.proto)

^{:line 4 :file "/tmp/cg3/proto.bclj"} (defprotocol Shape
  (area [self])
  (perim [self]))

^{:line 8 :file "/tmp/cg3/proto.bclj"} (defn describe [x]
  ^{:line 8 :file "/tmp/cg3/proto.bclj"} (+ x 100))
