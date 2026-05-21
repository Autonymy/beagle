;; Assumes `r` is the response namespace.
(when-not (= (r/greet "World") "Hello, World!")
  (println "FAIL: (greet \"World\") =>" (pr-str (r/greet "World")))
  (System/exit 1))
(when-not (= (r/greet "Tom") "Hello, Tom!")
  (println "FAIL: (greet \"Tom\") =>" (pr-str (r/greet "Tom")))
  (System/exit 1))
(System/exit 0)
