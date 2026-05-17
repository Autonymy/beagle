(ns trading.ledger
  (:require [trading.accounts :refer :all :as acct]
            [trading.trades :refer :all :as trd]))

^{:line 10 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defrecord LedgerEntry [id account-id ref-id entry-type amount balance-after timestamp])

(defn ledgerentry-id [r] (:id r))

(defn ledgerentry-account-id [r] (:account-id r))

(defn ledgerentry-ref-id [r] (:ref-id r))

(defn ledgerentry-entry-type [r] (:entry-type r))

(defn ledgerentry-amount [r] (:amount r))

(defn ledgerentry-balance-after [r] (:balance-after r))

(defn ledgerentry-timestamp [r] (:timestamp r))

^{:line 22 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn record-trade [id account trade ts]
  ^{:line 24 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [net ^{:line 24 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (trd/trade-net-amount trade)
   new-balance ^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (if ^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (< net 0) ^{:line 26 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (- ^{:line 26 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-balance account) ^{:line 27 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (- 0 net)) ^{:line 28 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (+ ^{:line 28 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-balance account) net))]
  ^{:line 30 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (->LedgerEntry id ^{:line 31 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-id account) ^{:line 32 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (trade-id trade) "trade" net new-balance ts)))

^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn record-deposit [id account amt ts]
  ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [new-bal ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (+ ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-balance account) amt)]
  ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (->LedgerEntry id ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-id account) 0 "deposit" amt new-bal ts)))

^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn record-withdrawal [id account amt ts]
  ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [new-bal ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (- ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-balance account) amt)]
  ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (->LedgerEntry id ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-id account) 0 "withdrawal" ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (- 0 amt) new-bal ts)))

^{:line 66 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn entries-for-account [entries acct-id]
  ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (filterv ^{:line 69 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (fn [e] ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (= ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (ledgerentry-account-id e) acct-id)) entries))

^{:line 74 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn entries-by-type [entries entry-type]
  ^{:line 76 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (filterv ^{:line 77 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (fn [e] ^{:line 78 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (= ^{:line 78 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (ledgerentry-entry-type e) entry-type)) entries))

^{:line 82 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn net-flow [entries]
  ^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [amounts ^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (mapv ^{:line 83 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (fn [e] ^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (ledgerentry-amount e)) entries)]
  ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (reduce + 0 amounts)))

^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn reconcile [entries account]
  ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [acct-entries ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (entries-for-account entries ^{:line 90 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-id account))
   computed-flow ^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (net-flow acct-entries)
   initial-offset ^{:line 92 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (- ^{:line 92 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (account-balance account) computed-flow)]
  ^{:line 94 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (>= initial-offset 0)))

^{:line 96 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (defn last-entry-balance [entries acct-id]
  ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (let [acct-entries ^{:line 97 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (entries-for-account entries acct-id)]
  ^{:line 98 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (if ^{:line 98 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (empty? acct-entries) 0 ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (ledgerentry-balance-after ^{:line 100 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run2/ledger.rkt"} (last acct-entries)))))
