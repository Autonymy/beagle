#lang typed/racket

;; Spike 2: defunion + exhaustive match
;;
;; Beagle source:
;;   (defunion Shape
;;     (Circle [(radius : Float)])
;;     (Rect [(w : Float) (h : Float)]))
;;
;;   (defn area [(s : Shape)] : Float
;;     (match s
;;       [(Circle r) (* 3.14159 r r)]
;;       [(Rect w h) (* w h)]))

(struct Circle ([radius : Flonum]) #:transparent)
(struct Rect ([w : Flonum] [h : Flonum]) #:transparent)

(define-type Shape (U Circle Rect))

(: area (-> Shape Flonum))
(define (area s)
  (cond
    [(Circle? s) (* 3.14159 (Circle-radius s) (Circle-radius s))]
    [(Rect? s)   (* (Rect-w s) (Rect-h s))]))

;; --- exercise it ---

(define c : Shape (Circle 5.0))
(define r : Shape (Rect 3.0 4.0))

(ann (area c) Flonum)
(ann (area r) Flonum)

(displayln (area c))  ; ~78.5
(displayln (area r))  ; 12.0
