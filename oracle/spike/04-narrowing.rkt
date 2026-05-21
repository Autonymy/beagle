#lang typed/racket

;; Spike 4: Flow-sensitive narrowing + nullable types
;;
;; Beagle source:
;;   (defn safe-name [(x : String?)] : String
;;     (if (nil? x) "unknown" x))
;;
;;   (defn process [(items : (Vec (U Circle Rect)))] : (Vec Float)
;;     (mapv (fn [s]
;;             (cond
;;               [(Circle? s) (Circle-radius s)]
;;               [(Rect? s) (Rect-w s)]))
;;           items))

;; Nullable narrowing
(: safe-name (-> (Option String) String))
(define (safe-name x)
  (if (not x) "unknown" x))

;; Union narrowing after predicate check
(struct Circle ([radius : Flonum]) #:transparent)
(struct Rect ([w : Flonum] [h : Flonum]) #:transparent)
(define-type Shape (U Circle Rect))

(: primary-dimension (-> Shape Flonum))
(define (primary-dimension s)
  (cond
    [(Circle? s) (Circle-radius s)]
    [(Rect? s)   (Rect-w s)]))

;; Narrowing in let-binding context
(: safe-divide (-> Integer Integer (Option Flonum)))
(define (safe-divide a b)
  (if (= b 0) #f (exact->inexact (/ a b))))

(: display-division (-> Integer Integer String))
(define (display-division a b)
  (define result (safe-divide a b))
  (if result
      (string-append "result: " (number->string result))
      "division by zero"))

;; --- exercise it ---

(ann (safe-name #f) String)
(ann (safe-name "alice") String)

(displayln (safe-name #f))       ; "unknown"
(displayln (safe-name "alice"))  ; "alice"

(displayln (primary-dimension (Circle 5.0)))  ; 5.0
(displayln (primary-dimension (Rect 3.0 4.0)))  ; 3.0

(displayln (display-division 10 3))  ; "result: 3.333..."
(displayln (display-division 10 0))  ; "division by zero"
