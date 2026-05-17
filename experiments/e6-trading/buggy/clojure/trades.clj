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

;; BUG-4: uses order-id as trade-id; BUG-19: only takes 3 params (missing ts); BUG-31: fee is 1bps not 10bps
(defn execute-trade [id order exec-price]
  (let [qty (order-quantity order)
   total-raw (* exec-price qty)
   fee-raw (quot (* total-raw 1) 10000)]
  (->Trade (order-id order) (order-id order) (order-account-id order) (order-instrument-id order) (order-side order) qty exec-price total-raw fee-raw 0)))

;; BUG-5: uses exec-price where qty expected; BUG-34: no sell-side flip
(defn trade-pnl [trade current-price]
  (let [exec (trade-exec-price trade)
   curr current-price
   qty (trade-exec-price trade)
   diff (- curr exec)]
  (* diff qty)))

;; BUG-7: returns fee instead of net amount
(defn trade-net-amount [trade]
  (trade-fee trade))

;; BUG-6: uses trade-id to look up account (wrong field for account lookup)
(defn settle-trade [trade account]
  (let [net (trade-net-amount trade)
   acct-id (trade-id trade)]
  (if (< net 0) (acct/debit-account account (- 0 net)) (acct/credit-account account net))))

;; BUG-22: uses trade-instrument-id instead of trade-account-id
(defn trades-for-account [trades acct-id]
  (filterv (fn [t] (= (trade-instrument-id t) acct-id)) trades))

(defn trades-for-instrument [trades inst-id]
  (filterv (fn [t] (= (trade-instrument-id t) inst-id)) trades))

(defn total-fees [trades]
  (let [fee-vals (mapv (fn [t] (trade-fee t)) trades)]
  (reduce + 0 fee-vals)))

;; BUG-28: uses exec-price where quantity expected (binding-accessor mismatch)
(defn avg-exec-price [trades]
  (if (empty? trades) 0 (let [total-cost (reduce + 0 (mapv (fn [t] (* (trade-exec-price t) (trade-exec-price t))) trades))
   total-qty (reduce + 0 (mapv (fn [t] (trade-exec-price t)) trades))]
  (quot total-cost total-qty))))
