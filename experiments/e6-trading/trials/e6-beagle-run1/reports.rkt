#lang beagle

(ns trading.reports)

(require trading.accounts :as acct)
(require trading.instruments :as inst)
(require trading.orders :as ord)
(require trading.trades :as trd)
(require trading.ledger :as ldg)

;; --- records ---

(defrecord Position
  [(instrument-id : InstrumentId)
   (ticker : Ticker)
   (quantity : Quantity)
   (avg-price : Price)
   (current-value : Amount)])

(defrecord PnLReport
  [(account-id : AccountId)
   (total-pnl : Amount)
   (total-fees : Amount)
   (num-trades : Long)
   (as-of : Timestamp)])

;; --- position calculation ---

;; BUG-11: fixed — uses net-qty for value; BUG-38: fixed — subtracts sell from buy
(defn calculate-position
  [(trades : (Vec Trade)) (inst-id : InstrumentId) (current-price : Price)
   (ticker : Ticker)] : Position
  (let [inst-trades (trd/trades-for-instrument trades inst-id)
        buy-trades (filterv (fn [(t : Trade)] : Boolean (= (trade-side t) "buy")) inst-trades)
        sell-trades (filterv (fn [(t : Trade)] : Boolean (= (trade-side t) "sell")) inst-trades)
        buy-qty (reduce + 0 (mapv (fn [(t : Trade)] : Long (quantity-value (trade-quantity t))) buy-trades))
        sell-qty (reduce + 0 (mapv (fn [(t : Trade)] : Long (quantity-value (trade-quantity t))) sell-trades))
        net-qty (- buy-qty sell-qty)
        avg (trd/avg-exec-price buy-trades)
        value (* (price-value current-price) net-qty)]
    (->Position inst-id ticker (->Quantity net-qty) avg (->Amount value))))

;; BUG-12: fixed — accumulates current-value
(defn portfolio-value [(positions : (Vec Position))] : Amount
  (let [values (mapv (fn [(p : Position)] : Long
                       (amount-value (position-current-value p)))
                     positions)]
    (->Amount (reduce + 0 values))))

;; --- PnL ---

;; BUG-13: fixed — sums trade pnl amounts per instrument
(defn total-pnl [(trades : (Vec Trade)) (current-prices : (Vec Price))
                 (instrument-ids : (Vec InstrumentId))] : Amount
  (let [pnls (mapv (fn [(i : Long)] : Long
                     (let [inst-id (nth instrument-ids i)
                           price (nth current-prices i)
                           inst-trades (trd/trades-for-instrument trades inst-id)
                           trade-pnls (mapv (fn [(t : Trade)] : Long
                                             (amount-value (trd/trade-pnl t price)))
                                           inst-trades)]
                       (reduce + 0 trade-pnls)))
                   (range (count instrument-ids)))]
    (->Amount (reduce + 0 pnls))))

;; --- report generation ---

;; BUG-30: fixed — passes ts (Timestamp)
(defn generate-pnl-report
  [(acct-id : AccountId) (trades : (Vec Trade)) (current-prices : (Vec Price))
   (instrument-ids : (Vec InstrumentId)) (ts : Timestamp)] : PnLReport
  (let [acct-trades (trd/trades-for-account trades acct-id)
        pnl (total-pnl acct-trades current-prices instrument-ids)
        fees (trd/total-fees acct-trades)
        n (count acct-trades)]
    (->PnLReport acct-id pnl fees n ts)))

;; --- formatting ---

(defn format-position [(pos : Position)] : String
  (let [t (ticker-value (position-ticker pos))
        q (quantity-value (position-quantity pos))
        v (amount-value (position-current-value pos))
        dollars (quot v 100)
        cents (mod (if (< v 0) (- 0 v) v) 100)]
    (str t ": " q " units, value " dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-21: fixed — uses pnl-val directly
(defn format-pnl-report [(report : PnLReport)] : String
  (let [pnl-val (amount-value (pnlreport-total-pnl report))
        fees-val (amount-value (pnlreport-total-fees report))
        net (- pnl-val fees-val)
        dollars (quot net 100)
        cents (mod (if (< net 0) (- 0 net) net) 100)]
    (str "PnL Report: "
         (pnlreport-num-trades report) " trades, "
         "net " (if (< net 0) "-" "") dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-26: fixed — format-balance called with 1 arg
(defn account-summary
  [(account : Account) (trades : (Vec Trade)) (entries : (Vec LedgerEntry))] : String
  (let [acct-id (account-id account)
        acct-trades (trd/trades-for-account trades acct-id)
        acct-entries (ldg/entries-for-account entries acct-id)
        balance (acct/format-balance account)
        num-trades (count acct-trades)
        num-entries (count acct-entries)]
    (str (account-name account) " | "
         balance " | "
         num-trades " trades | "
         num-entries " ledger entries")))
