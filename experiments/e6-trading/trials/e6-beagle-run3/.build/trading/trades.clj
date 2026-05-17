(ns trading.trades
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]
            [trading.orders :refer :all :as ord]))

;; TradeId : Long (scalar)

^{:line 15 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defrecord Trade [id order-id account-id instrument-id side quantity exec-price total fee executed-at])

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

^{:line 29 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn execute-trade [id order exec-price ts]
  ^{:line 32 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [qty ^{:line 32 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (order-quantity order)
   total-raw ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (* exec-price qty)
   fee-raw ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (quot ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (* total-raw 10) 10000)]
  ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (->Trade id ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (order-id order) ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (order-account-id order) ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (order-instrument-id order) ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (order-side order) qty exec-price total-raw fee-raw ts)))

^{:line 49 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn trade-pnl [trade current-price]
  ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [exec ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-exec-price trade)
   curr current-price
   qty ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-quantity trade)
   diff ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (- curr exec)]
  ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (if ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (= ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-side trade) "sell") ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (* ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (- 0 diff) qty) ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (* diff qty))))

^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn trade-net-amount [trade]
  ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [total ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-total trade)
   fee ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-fee trade)]
  ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (if ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (= ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-side trade) "buy") ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (- 0 ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (+ total fee)) ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (- total fee))))

^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn settle-trade [trade account]
  ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [net ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-net-amount trade)]
  ^{:line 69 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (if ^{:line 69 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (< net 0) ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (acct/debit-account account ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (- 0 net)) ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (acct/credit-account account net))))

^{:line 74 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn trades-for-account [trades acct-id]
  ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (filterv ^{:line 76 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (fn [t] ^{:line 77 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (= ^{:line 77 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-account-id t) acct-id)) trades))

^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn trades-for-instrument [trades inst-id]
  ^{:line 82 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (filterv ^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (fn [t] ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (= ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-instrument-id t) inst-id)) trades))

^{:line 88 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn total-fees [trades]
  ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [fee-vals ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (mapv ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (fn [t] ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-fee t)) trades)]
  ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (reduce + 0 fee-vals)))

^{:line 93 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (defn avg-exec-price [trades]
  ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (if ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (empty? trades) 0 ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (let [total-cost ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (reduce + 0 ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (mapv ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (fn [t] ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (* ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-exec-price t) ^{:line 98 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-quantity t))) trades))
   total-qty ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (reduce + 0 ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (mapv ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (fn [t] ^{:line 101 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (trade-quantity t)) trades))]
  ^{:line 103 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/trades.rkt"} (quot total-cost total-qty))))
