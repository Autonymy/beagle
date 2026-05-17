(ns trading.orders
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]))

;; OrderId : Long (scalar)

^{:line 14 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defrecord Order [id account-id instrument-id side quantity limit-price status created-at filled-at])

(defn order-id [r] (:id r))

(defn order-account-id [r] (:account-id r))

(defn order-instrument-id [r] (:instrument-id r))

(defn order-side [r] (:side r))

(defn order-quantity [r] (:quantity r))

(defn order-limit-price [r] (:limit-price r))

(defn order-status [r] (:status r))

(defn order-created-at [r] (:created-at r))

(defn order-filled-at [r] (:filled-at r))

^{:line 28 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn create-order [id account-id instrument-id side qty price ts]
  ^{:line 31 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (->Order id account-id instrument-id side qty price "pending" ts nil))

^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn order-total [order]
  ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (let [p ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-limit-price order)
   q ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-quantity order)]
  ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (* p q)))

^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn validate-order [order account instrument]
  ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (let [total ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-total order)
   valid-qty ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (inst/valid-quantity? instrument ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-quantity order))
   has-funds ^{:line 47 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (acct/sufficient-balance? account total)]
  ^{:line 48 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (cond
  ^{:line 49 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (not valid-qty) "invalid quantity: must be multiple of lot size"
  ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (and ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (= ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-side order) "buy") ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (not has-funds)) "insufficient balance"
  :else nil)))

^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn fill-order [order fill-ts]
  ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (assoc ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (assoc order :status "filled") :filled-at fill-ts))

^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn cancel-order [order reason]
  ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (assoc order :status ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (str "cancelled:" reason)))

^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn order-active? [order]
  ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (= ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-status order) "pending"))

^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn orders-for-account [orders acct-id]
  ^{:line 65 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (filterv ^{:line 66 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (fn [o] ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (= ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-account-id o) acct-id)) orders))

^{:line 72 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn pending-orders [orders]
  ^{:line 73 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (filterv ^{:line 74 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (fn [o] ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (= ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-status o) "pending")) orders))

^{:line 79 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (defn total-exposure [orders]
  ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (let [totals ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (mapv ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (fn [o] ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (order-total o)) ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (pending-orders orders))]
  ^{:line 82 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt"} (reduce + 0 totals)))
