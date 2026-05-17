#lang beagle

(ns trading.orders)

(require trading.accounts :as acct)
(require trading.instruments :as inst)

;; --- scalar types ---

(defscalar OrderId Long)

;; --- records ---

(defrecord Order
  [(id : OrderId)
   (account-id : AccountId)
   (instrument-id : InstrumentId)
   (side : String)
   (quantity : Quantity)
   (limit-price : Price)
   (status : String)
   (created-at : Timestamp)
   (filled-at : (U Timestamp Nil))])

;; --- constructors ---

(defn create-order
  [(id : Long) (account-id : AccountId) (instrument-id : InstrumentId)
   (side : String) (qty : Quantity) (price : Price) (ts : Timestamp)] : Order
  (->Order (->OrderId id) account-id instrument-id
           side qty price "pending" ts nil))

;; --- operations ---

(defn order-total [(order : Order)] : Amount
  (let [p (price-value (order-limit-price order))
        q (quantity-value (order-quantity order))]
    (->Amount (* p q))))

(defn validate-order
  [(order : Order) (account : Account) (instrument : Instrument)] : (U String Nil)
  (let [total (order-total order)
        valid-qty (inst/valid-quantity? instrument (order-quantity order))
        is-buy (= (order-side order) "buy")
        has-funds (acct/sufficient-balance? account total)]
    (cond
      [(not valid-qty) "invalid quantity: must be multiple of lot size"]
      [(and is-buy (not has-funds)) "insufficient balance"]
      [:else nil])))

(defn fill-order [(order : Order) (fill-ts : Timestamp)] : Order
  (with order [:status "filled"] [:filled-at fill-ts]))

(defn cancel-order [(order : Order) (reason : String)] : Order
  (with order [:status (str "cancelled:" reason)]))

(defn order-active? [(order : Order)] : Boolean
  (= (order-status order) "pending"))

(defn orders-for-account [(orders : (Vec Order)) (acct-id : AccountId)] : (Vec Order)
  (filterv
    (fn [(o : Order)] : Boolean
      (= (accountid-value (order-account-id o))
         (accountid-value acct-id)))
    orders))

(defn pending-orders [(orders : (Vec Order))] : (Vec Order)
  (filterv
    (fn [(o : Order)] : Boolean (= (order-status o) "pending"))
    orders))

(defn total-exposure [(orders : (Vec Order))] : Amount
  (let [totals (mapv (fn [(o : Order)] : Long (amount-value (order-total o)))
                     (pending-orders orders))]
    (->Amount (reduce + 0 totals))))
