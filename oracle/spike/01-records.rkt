#lang typed/racket

;; Spike 1: defrecord → Typed Racket struct
;;
;; Beagle source:
;;   (defrecord Product [(name : String) (price : Int) (stock : Int)])
;;   (def p : Product (->Product "Widget" 999 100))
;;   (product-name p)  ; => "Widget"

(struct Product ([name : String] [price : Integer] [stock : Integer])
  #:transparent)

(: make-product (-> String Integer Integer Product))
(define (make-product name price stock)
  (Product name price stock))

(: product-expensive? (-> Product Boolean))
(define (product-expensive? p)
  (> (Product-price p) 1000))

;; --- exercise it ---

(define p : Product (make-product "Widget" 999 100))

(ann (Product-name p) String)
(ann (Product-price p) Integer)
(ann (product-expensive? p) Boolean)

(displayln (Product-name p))
(displayln (product-expensive? p))
