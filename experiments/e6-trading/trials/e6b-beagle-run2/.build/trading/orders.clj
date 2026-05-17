(ns trading.orders
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]))

;; OrderId : Long (scalar)

^{:line 14 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defrecord Order [id account-id instrument-id side quantity limit-price status created-at filled-at])

(defn order-id [r] (:id r))

(defn order-account-id [r] (:account-id r))

(defn order-instrument-id [r] (:instrument-id r))

(defn order-side [r] (:side r))

(defn order-quantity [r] (:quantity r))

(defn order-limit-price [r] (:limit-price r))

(defn order-status [r] (:status r))

(defn order-created-at [r] (:created-at r))

(defn order-filled-at [r] (:filled-at r))

^{:line 27 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn create-order [id account-id instrument-id side qty price ts]
  ^{:line 30 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (->Order id account-id instrument-id side qty price "pending" ts nil))

^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn order-total [order]
  ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (let [p ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-limit-price order)
   q ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-quantity order)]
  ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (* p q)))

^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn validate-order [order account instrument]
  ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (let [total ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-total order)
   valid-qty ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (inst/valid-quantity? instrument ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-quantity order))
   is-buy ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (= ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-side order) "buy")
   has-funds ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (or ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (not is-buy) ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (acct/sufficient-balance? account total))]
  ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (cond
  ^{:line 47 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (not valid-qty) "invalid quantity: must be multiple of lot size"
  ^{:line 48 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (not has-funds) "insufficient balance"
  :else nil)))

^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn fill-order [order fill-ts]
  ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (assoc order :status "filled" :filled-at fill-ts))

^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn cancel-order [order reason]
  ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (assoc order :status ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (str "cancelled:" reason)))

^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn order-active? [order]
  ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (= ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-status order) "pending"))

^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn orders-for-account [orders acct-id]
  ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (filterv ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (fn [o] ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (= ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-account-id o) acct-id)) orders))

^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn pending-orders [orders]
  ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (filterv ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (fn [o] ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (= ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-status o) "pending")) orders))

^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (defn total-exposure [orders]
  ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (let [totals ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (mapv ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (fn [o] ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (order-total o)) ^{:line 72 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (pending-orders orders))]
  ^{:line 73 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/orders.rkt"} (reduce + 0 totals)))
