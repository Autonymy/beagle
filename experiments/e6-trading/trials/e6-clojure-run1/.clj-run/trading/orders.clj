(ns trading.orders
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]))

(defrecord Order [id account-id instrument-id side quantity limit-price status created-at filled-at])

(defn order-id [r] (:id r))

(defn order-account-id [r] (:account-id r))

(defn order-instrument-id [r] (:instrument-id r))

(defn order-side [r] (:side r))

(defn order-quantity [r] (:quantity r))

(defn order-limit-price [r] (:limit-price r))

(defn order-status [r] (:status r))

(defn order-created-at [r] (:created-at r))

(defn order-filled-at [r] (:filled-at r))

;; BUG-1 FIXED: correct field assignment
(defn create-order [id account-id instrument-id side qty price ts]
  (->Order id account-id instrument-id side qty price "pending" ts nil))

;; BUG-2 FIXED: returns price*qty
(defn order-total [order]
  (let [p (order-limit-price order)
   q (order-quantity order)]
  (* p q)))

;; BUG-3 FIXED: uses order-total; BUG-35 FIXED: checks buy side
(defn validate-order [order account instrument]
  (let [total (order-total order)
   valid-qty (inst/valid-quantity? instrument (order-quantity order))
   is-buy (= (order-side order) "buy")
   has-funds (acct/sufficient-balance? account total)]
  (cond
  (not valid-qty) "invalid quantity: must be multiple of lot size"
  (and is-buy (not has-funds)) "insufficient balance"
  :else nil)))

;; BUG-27 FIXED: sets :filled-at field
(defn fill-order [order fill-ts]
  (assoc order :status "filled" :filled-at fill-ts))

(defn cancel-order [order reason]
  (assoc order :status (str "cancelled:" reason)))

(defn order-active? [order]
  (= (order-status order) "pending"))

;; BUG-20 FIXED: uses order-account-id
(defn orders-for-account [orders acct-id]
  (filterv (fn [o] (= (order-account-id o) acct-id)) orders))

;; BUG-36 FIXED: filters for pending only
(defn pending-orders [orders]
  (filterv (fn [o] (= (order-status o) "pending")) orders))

;; BUG-18 FIXED: sums order totals
(defn total-exposure [orders]
  (let [totals (mapv (fn [o] (order-total o)) (pending-orders orders))]
  (reduce + 0 totals)))
