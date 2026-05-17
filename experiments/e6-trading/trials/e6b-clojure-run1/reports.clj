(ns trading.reports
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]
            [trading.orders :refer :all :as ord]
            [trading.trades :refer :all :as trd]
            [trading.ledger :refer :all :as ldg]))

(defrecord Position [instrument-id ticker quantity avg-price current-value])

(defn position-instrument-id [r] (:instrument-id r))

(defn position-ticker [r] (:ticker r))

(defn position-quantity [r] (:quantity r))

(defn position-avg-price [r] (:avg-price r))

(defn position-current-value [r] (:current-value r))

(defrecord PnLReport [account-id total-pnl total-fees num-trades as-of])

(defn pnlreport-account-id [r] (:account-id r))

(defn pnlreport-total-pnl [r] (:total-pnl r))

(defn pnlreport-total-fees [r] (:total-fees r))

(defn pnlreport-num-trades [r] (:num-trades r))

(defn pnlreport-as-of [r] (:as-of r))

(defn calculate-position [trades inst-id current-price ticker]
  (let [inst-trades (trd/trades-for-instrument trades inst-id)
        buy-trades (filterv (fn [t] (= (trade-side t) "buy")) inst-trades)
        sell-trades (filterv (fn [t] (= (trade-side t) "sell")) inst-trades)
        buy-qty (reduce + 0 (mapv (fn [t] (trade-quantity t)) buy-trades))
        sell-qty (reduce + 0 (mapv (fn [t] (trade-quantity t)) sell-trades))
        net-qty (- buy-qty sell-qty)
        avg (trd/avg-exec-price buy-trades)
        value (* current-price net-qty)]
    (->Position inst-id ticker net-qty avg value)))

(defn portfolio-value [positions]
  (let [values (mapv (fn [p] (position-current-value p)) positions)]
    (reduce + 0 values)))

(defn total-pnl [trades current-prices instrument-ids]
  (let [pnls (mapv (fn [i] (let [inst-id (nth instrument-ids i)
                                 price (nth current-prices i)
                                 inst-trades (trd/trades-for-instrument trades inst-id)
                                 trade-pnls (mapv (fn [t] (trd/trade-pnl t price)) inst-trades)]
                             (reduce + 0 trade-pnls))) (range (count instrument-ids)))]
    (reduce + 0 pnls)))

(defn generate-pnl-report [acct-id trades current-prices instrument-ids ts]
  (let [acct-trades (trd/trades-for-account trades acct-id)
        pnl (total-pnl acct-trades current-prices instrument-ids)
        fees (trd/total-fees acct-trades)
        n (count acct-trades)]
    (->PnLReport acct-id pnl fees n ts)))

(defn format-position [pos]
  (let [t (position-ticker pos)
        q (position-quantity pos)
        v (position-current-value pos)
        dollars (quot v 100)
        cents (mod (if (< v 0) (- 0 v) v) 100)]
    (str t ": " q " units, value " dollars "." (if (< cents 10) "0" "") cents)))

(defn format-pnl-report [report]
  (let [pnl-val (pnlreport-total-pnl report)
        fees-val (pnlreport-total-fees report)
        net (- pnl-val fees-val)
        dollars (quot net 100)
        cents (mod (if (< net 0) (- 0 net) net) 100)]
    (str "PnL Report: " (pnlreport-num-trades report) " trades, " "net " (if (< net 0) "-" "") dollars "." (if (< cents 10) "0" "") cents)))

(defn account-summary [account trades entries]
  (let [acct-id (account-id account)
        acct-trades (trd/trades-for-account trades acct-id)
        acct-entries (ldg/entries-for-account entries acct-id)
        balance (acct/format-balance account)
        num-trades (count acct-trades)
        num-entries (count acct-entries)]
    (str (account-name account) " | " balance " | " num-trades " trades | " num-entries " ledger entries")))
