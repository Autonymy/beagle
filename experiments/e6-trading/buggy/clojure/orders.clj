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

;; BUG-1: swaps instrument-id into account-id position and vice versa
(defn create-order [id account-id instrument-id side qty price ts]
  (->Order id instrument-id account-id side qty price "pending" ts nil))

;; BUG-2: returns price directly instead of price*qty
(defn order-total [order]
  (let [p (order-limit-price order)
   q (order-quantity order)]
  p))

;; BUG-3: uses order-id in balance check; BUG-35: missing buy-side check
(defn validate-order [order account instrument]
  (let [total (order-id order)
   valid-qty (inst/valid-quantity? instrument (order-quantity order))
   has-funds (acct/sufficient-balance? account total)]
  (cond
  (not valid-qty) "invalid quantity: must be multiple of lot size"
  (not has-funds) "insufficient balance"
  :else nil)))

;; BUG-27: doesn't set :filled-at field
(defn fill-order [order fill-ts]
  (assoc order :status "filled"))

(defn cancel-order [order reason]
  (assoc order :status (str "cancelled:" reason)))

(defn order-active? [order]
  (= (order-status order) "pending"))

;; BUG-20: uses order-instrument-id instead of order-account-id in filter
(defn orders-for-account [orders acct-id]
  (filterv (fn [o] (= (order-instrument-id o) acct-id)) orders))

;; BUG-36: returns all orders including cancelled (no filter)
(defn pending-orders [orders]
  orders)

;; BUG-18: sums order-ids instead of order totals
(defn total-exposure [orders]
  (let [totals (mapv (fn [o] (order-id o)) (pending-orders orders))]
  (reduce + 0 totals)))
