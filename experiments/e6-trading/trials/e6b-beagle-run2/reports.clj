(ns trading.reports
  (:require [trading.accounts :refer :all :as acct]
            [trading.instruments :refer :all :as inst]
            [trading.orders :refer :all :as ord]
            [trading.trades :refer :all :as trd]
            [trading.ledger :refer :all :as ldg]))

^{:line 13 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defrecord Position [instrument-id ticker quantity avg-price current-value])

(defn position-instrument-id [r] (:instrument-id r))

(defn position-ticker [r] (:ticker r))

(defn position-quantity [r] (:quantity r))

(defn position-avg-price [r] (:avg-price r))

(defn position-current-value [r] (:current-value r))

^{:line 20 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defrecord PnLReport [account-id total-pnl total-fees num-trades as-of])

(defn pnlreport-account-id [r] (:account-id r))

(defn pnlreport-total-pnl [r] (:total-pnl r))

(defn pnlreport-total-fees [r] (:total-fees r))

(defn pnlreport-num-trades [r] (:num-trades r))

(defn pnlreport-as-of [r] (:as-of r))

^{:line 29 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn calculate-position [trades inst-id current-price ticker]
  ^{:line 32 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [inst-trades ^{:line 32 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/trades-for-instrument trades inst-id)
   buy-trades ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (filterv ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [t] ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (= ^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trade-side t) "buy")) inst-trades)
   sell-trades ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (filterv ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [t] ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (= ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trade-side t) "sell")) inst-trades)
   buy-qty ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (reduce + 0 ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mapv ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [t] ^{:line 35 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trade-quantity t)) buy-trades))
   sell-qty ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (reduce + 0 ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mapv ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [t] ^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trade-quantity t)) sell-trades))
   net-qty ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (- buy-qty sell-qty)
   avg ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/avg-exec-price buy-trades)
   value ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (* current-price net-qty)]
  ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (->Position inst-id ticker net-qty avg value)))

^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn portfolio-value [positions]
  ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [values ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mapv ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [p] ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (position-current-value p)) positions)]
  ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (reduce + 0 values)))

^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn total-pnl [trades current-prices instrument-ids]
  ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [pnls ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mapv ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [i] ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [inst-id ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (nth instrument-ids i)
   price ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (nth current-prices i)
   inst-trades ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/trades-for-instrument trades inst-id)]
  ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (reduce + 0 ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mapv ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (fn [t] ^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/trade-pnl t price)) inst-trades)))) ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (range ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (count instrument-ids)))]
  ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (reduce + 0 pnls)))

^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn generate-pnl-report [acct-id trades current-prices instrument-ids ts]
  ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [acct-trades ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/trades-for-account trades acct-id)
   pnl ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (total-pnl acct-trades current-prices instrument-ids)
   fees ^{:line 69 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/total-fees acct-trades)
   n ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (count acct-trades)]
  ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (->PnLReport acct-id pnl fees n ts)))

^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn format-position [pos]
  ^{:line 76 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [t ^{:line 76 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (position-ticker pos)
   q ^{:line 77 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (position-quantity pos)
   v ^{:line 78 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (position-current-value pos)
   dollars ^{:line 79 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (quot v 100)
   cents ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mod ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (if ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (< v 0) ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (- 0 v) v) 100)]
  ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (str t ": " q " units, value " dollars "." ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (if ^{:line 81 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (< cents 10) "0" "") cents)))

^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn format-pnl-report [report]
  ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [pnl-val ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (pnlreport-total-pnl report)
   fees-val ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (pnlreport-total-fees report)
   net ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (- pnl-val fees-val)
   dollars ^{:line 87 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (quot net 100)
   cents ^{:line 88 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (mod ^{:line 88 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (if ^{:line 88 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (< net 0) ^{:line 88 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (- 0 net) net) 100)]
  ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (str "PnL Report: " ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (pnlreport-num-trades report) " trades, " "net " ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (if ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (< net 0) "-" "") dollars "." ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (if ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (< cents 10) "0" "") cents)))

^{:line 93 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (defn account-summary [account trades entries]
  ^{:line 95 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (let [acct-id ^{:line 95 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (account-id account)
   acct-trades ^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (trd/trades-for-account trades acct-id)
   acct-entries ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (ldg/entries-for-account entries acct-id)
   balance ^{:line 98 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (acct/format-balance account)
   num-trades ^{:line 99 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (count acct-trades)
   num-entries ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (count acct-entries)]
  ^{:line 101 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (str ^{:line 101 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/reports.rkt"} (account-name account) " | " balance " | " num-trades " trades | " num-entries " ledger entries")))
