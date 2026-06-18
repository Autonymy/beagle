(ns c8.main
  (:require [c8.util :as u]))

^{:line 8 :file "/tmp/cgtest.QQCMy2/c8/main.bclj"} (defn hof-use [xs]
  ^{:line 9 :file "/tmp/cgtest.QQCMy2/c8/main.bclj"} (map u/dbl xs))

^{:line 12 :file "/tmp/cgtest.QQCMy2/c8/main.bclj"} (defn direct-use [n]
  ^{:line 13 :file "/tmp/cgtest.QQCMy2/c8/main.bclj"} (u/dbl n))
