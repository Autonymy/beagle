(ns trading.trades
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]
            [trading.orders :refer :all :as ord]))

(defrecord Trade [id order-id account-id instrument-id side quantity exec-price total fee executed-at])

(defn trade-id [r] (:id r))

(defn trade-order-id [r] (:order-id r))

(defn trade-account-id [r] (:account-id r))

(defn trade-instrument-id [r] (:instrument-id r))

(defn trade-side [r] (:side r))

(defn trade-quantity [r] (:quantity r))

(defn trade-exec-price [r] (:exec-price r))

(defn trade-total [r] (:total r))

(defn trade-fee [r] (:fee r))

(defn trade-executed-at [r] (:executed-at r))

(defn execute-trade [id order exec-price ts]
  (let [qty (order-quantity order)
        total-raw (* exec-price qty)
        fee-raw (quot (* total-raw 10) 10000)]
    (->Trade id (order-id order) (order-account-id order) (order-instrument-id order) (order-side order) qty exec-price total-raw fee-raw ts)))

(defn trade-pnl [trade current-price]
  (let [exec (trade-exec-price trade)
        curr current-price
        qty (trade-quantity trade)
        diff (- curr exec)
        is-sell (= (trade-side trade) "sell")]
    (* (if is-sell (- 0 diff) diff) qty)))

(defn trade-net-amount [trade]
  (let [total (trade-total trade)
        fee (trade-fee trade)
        is-buy (= (trade-side trade) "buy")]
    (if is-buy (- 0 (+ total fee)) (- total fee))))

(defn settle-trade [trade account]
  (let [net (trade-net-amount trade)]
    (if (< net 0) (acct/debit-account account (- 0 net)) (acct/credit-account account net))))

(defn trades-for-account [trades acct-id]
  (filterv (fn [t] (= (trade-account-id t) acct-id)) trades))

(defn trades-for-instrument [trades inst-id]
  (filterv (fn [t] (= (trade-instrument-id t) inst-id)) trades))

(defn total-fees [trades]
  (let [fee-vals (mapv (fn [t] (trade-fee t)) trades)]
    (reduce + 0 fee-vals)))

(defn avg-exec-price [trades]
  (if (empty? trades) 0 (let [total-cost (reduce + 0 (mapv (fn [t] (* (trade-exec-price t) (trade-quantity t))) trades))
                              total-qty (reduce + 0 (mapv (fn [t] (trade-quantity t)) trades))]
                          (quot total-cost total-qty))))
