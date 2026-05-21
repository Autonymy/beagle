#lang typed/racket

;; Spike 3: Parametric defunion — Result T E / Option T
;;
;; Beagle source:
;;   (defunion (Result T E)
;;     (Ok  [(value : T)])
;;     (Err [(error : E)]))
;;
;;   (defn find-product [(id : Int) (catalog : (Vec Product))]
;;       : (Result Product String)
;;     (if-let [p (first (filterv ...))]
;;       (->Ok p)
;;       (->Err "not found")))

(struct (T) Ok ([value : T]) #:transparent)
(struct (E) Err ([error : E]) #:transparent)

(define-type (Result T E) (U (Ok T) (Err E)))

;; Option as a special case
(define-type (Option T) (U (Ok T) (Err Void)))

;; A record to use with Result
(struct Product ([name : String] [price : Integer]) #:transparent)

(: find-product (-> Integer (Listof Product) (Result Product String)))
(define (find-product id catalog)
  (define match
    (filter (λ ([p : Product]) (= (Product-price p) id)) catalog))
  (if (null? match)
      (Err "not found")
      (Ok (car match))))

(: describe-result (-> (Result Product String) String))
(define (describe-result r)
  (cond
    [(Ok? r)  (string-append "Found: " (Product-name (Ok-value r)))]
    [(Err? r) (string-append "Error: " (Err-error r))]))

;; unwrap with default
(: unwrap-or (All (T E) (-> (Result T E) T T)))
(define (unwrap-or r default)
  (if (Ok? r) (Ok-value r) default))

;; --- exercise it ---

(define catalog : (Listof Product)
  (list (Product "Widget" 999) (Product "Gadget" 2499)))

(define r1 (find-product 999 catalog))
(define r2 (find-product 0 catalog))

(ann r1 (Result Product String))
(ann r2 (Result Product String))

(displayln (describe-result r1))  ; "Found: Widget"
(displayln (describe-result r2))  ; "Error: not found"
(displayln (Product-name (unwrap-or r2 (Product "default" 0))))  ; "default"
