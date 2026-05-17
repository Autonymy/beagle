(ns trading.reports
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]
            [trading.orders :refer :all :as ord]
            [trading.trades :refer :all :as trd]
            [trading.ledger :refer :all :as ldg]))

^{:line 13 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defrecord Position [instrument-id ticker quantity avg-price current-value])

(defn position-instrument-id [r] (:instrument-id r))

(defn position-ticker [r] (:ticker r))

(defn position-quantity [r] (:quantity r))

(defn position-avg-price [r] (:avg-price r))

(defn position-current-value [r] (:current-value r))

^{:line 20 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defrecord PnLReport [account-id total-pnl total-fees num-trades as-of])

(defn pnlreport-account-id [r] (:account-id r))

(defn pnlreport-total-pnl [r] (:total-pnl r))

(defn pnlreport-total-fees [r] (:total-fees r))

(defn pnlreport-num-trades [r] (:num-trades r))

(defn pnlreport-as-of [r] (:as-of r))

^{:line 30 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn calculate-position [trades inst-id current-price ticker]
  ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [inst-trades ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/trades-for-instrument trades inst-id)
   buy-trades ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (filterv ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [t] ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (= ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trade-side t) "buy")) inst-trades)
   sell-trades ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (filterv ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [t] ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (= ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trade-side t) "sell")) inst-trades)
   buy-qty ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (reduce + 0 ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mapv ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [t] ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trade-quantity t)) buy-trades))
   sell-qty ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (reduce + 0 ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mapv ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [t] ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trade-quantity t)) sell-trades))
   net-qty ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (- buy-qty sell-qty)
   avg ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/avg-exec-price buy-trades)
   value ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (* current-price net-qty)]
  ^{:line 41 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (->Position inst-id ticker net-qty avg value)))

^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn portfolio-value [positions]
  ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [values ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mapv ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [p] ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (position-current-value p)) positions)]
  ^{:line 48 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (reduce + 0 values)))

^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn total-pnl [trades current-prices instrument-ids]
  ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [pnls ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mapv ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [i] ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [inst-id ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (nth instrument-ids i)
   price ^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (nth current-prices i)
   inst-trades ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/trades-for-instrument trades inst-id)
   trade-pnls ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mapv ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (fn [t] ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/trade-pnl t price)) inst-trades)]
  ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (reduce + 0 trade-pnls))) ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (range ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (count instrument-ids)))]
  ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (reduce + 0 pnls)))

^{:line 69 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn generate-pnl-report [acct-id trades current-prices instrument-ids ts]
  ^{:line 72 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [acct-trades ^{:line 72 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/trades-for-account trades acct-id)
   pnl ^{:line 73 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (total-pnl acct-trades current-prices instrument-ids)
   fees ^{:line 74 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/total-fees acct-trades)
   n ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (count acct-trades)]
  ^{:line 76 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (->PnLReport acct-id pnl fees n ts)))

^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn format-position [pos]
  ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [t ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (position-ticker pos)
   q ^{:line 82 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (position-quantity pos)
   v ^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (position-current-value pos)
   dollars ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (quot v 100)
   cents ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mod ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (if ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (< v 0) ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (- 0 v) v) 100)]
  ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (str t ": " q " units, value " dollars "." ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (if ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (< cents 10) "0" "") cents)))

^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn format-pnl-report [report]
  ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [pnl-val ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (pnlreport-total-pnl report)
   fees-val ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (pnlreport-total-fees report)
   net ^{:line 92 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (- pnl-val fees-val)
   dollars ^{:line 93 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (quot net 100)
   cents ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (mod ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (if ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (< net 0) ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (- 0 net) net) 100)]
  ^{:line 95 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (str "PnL Report: " ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (pnlreport-num-trades report) " trades, " "net " ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (if ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (< net 0) "-" "") dollars "." ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (if ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (< cents 10) "0" "") cents)))

^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (defn account-summary [account trades entries]
  ^{:line 102 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (let [acct-id ^{:line 102 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (account-id account)
   acct-trades ^{:line 103 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (trd/trades-for-account trades acct-id)
   acct-entries ^{:line 104 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (ldg/entries-for-account entries acct-id)
   balance ^{:line 105 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (acct/format-balance account)
   num-trades ^{:line 106 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (count acct-trades)
   num-entries ^{:line 107 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (count acct-entries)]
  ^{:line 108 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (str ^{:line 108 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run3/reports.rkt"} (account-name account) " | " balance " | " num-trades " trades | " num-entries " ledger entries")))
