;; Heron's formula on a 3-4-5 right triangle = 6.0
(let [result (r/triangle-area 3 4 5)]
  (when-not (and (number? result)
                 (< (Math/abs (- (double result) 6.0)) 0.01))
    (println "FAIL: (triangle-area 3 4 5) =>" (pr-str result))
    (System/exit 1)))
(System/exit 0)
