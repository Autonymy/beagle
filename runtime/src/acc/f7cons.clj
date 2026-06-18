(ns acc.f7cons
  (:require [acc.f7prod :as f7prod]))

^{:line 7 :file "/tmp/acc/chk-f7cons.bclj"} (defn mk [n]
  ^{:line 7 :file "/tmp/acc/chk-f7cons.bclj"} (acc.f7prod/->Box n))

^{:line 9 :file "/tmp/acc/chk-f7cons.bclj"} (defn getw [b]
  ^{:line 9 :file "/tmp/acc/chk-f7cons.bclj"} (acc.f7prod/box-w b))
