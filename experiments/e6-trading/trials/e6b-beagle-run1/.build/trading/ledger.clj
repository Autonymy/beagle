(ns trading.ledger
  (:require [trading.accounts :refer :all :as acct]
            [trading.trades :refer :all :as trd]))

^{:line 10 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defrecord LedgerEntry [id account-id ref-id entry-type amount balance-after timestamp])

(defn ledgerentry-id [r] (:id r))

(defn ledgerentry-account-id [r] (:account-id r))

(defn ledgerentry-ref-id [r] (:ref-id r))

(defn ledgerentry-entry-type [r] (:entry-type r))

(defn ledgerentry-amount [r] (:amount r))

(defn ledgerentry-balance-after [r] (:balance-after r))

(defn ledgerentry-timestamp [r] (:timestamp r))

^{:line 21 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn record-trade [id account trade ts]
  ^{:line 23 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [net ^{:line 23 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (trd/trade-net-amount trade)
   new-balance ^{:line 24 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (if ^{:line 24 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (< net 0) ^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (- ^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-balance account) ^{:line 26 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (- 0 net)) ^{:line 27 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (+ ^{:line 27 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-balance account) net))]
  ^{:line 29 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (->LedgerEntry id ^{:line 30 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-id account) ^{:line 31 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (trade-id trade) "trade" net new-balance ts)))

^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn record-deposit [id account amt ts]
  ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [new-bal ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (+ ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-balance account) amt)]
  ^{:line 41 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (->LedgerEntry id ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-id account) 0 "deposit" amt new-bal ts)))

^{:line 49 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn record-withdrawal [id account amt ts]
  ^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [new-bal ^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (- ^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-balance account) amt)]
  ^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (->LedgerEntry id ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-id account) 0 "withdrawal" ^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (- 0 amt) new-bal ts)))

^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn entries-for-account [entries acct-id]
  ^{:line 65 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (filterv ^{:line 66 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (fn [e] ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (= ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (ledgerentry-account-id e) acct-id)) entries))

^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn entries-by-type [entries entry-type]
  ^{:line 73 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (filterv ^{:line 74 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (fn [e] ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (= ^{:line 75 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (ledgerentry-entry-type e) entry-type)) entries))

^{:line 78 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn net-flow [entries]
  ^{:line 79 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [amounts ^{:line 79 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (mapv ^{:line 79 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (fn [e] ^{:line 80 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (ledgerentry-amount e)) entries)]
  ^{:line 82 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (reduce + 0 amounts)))

^{:line 84 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn reconcile [entries account]
  ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [acct-entries ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (entries-for-account entries ^{:line 85 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-id account))
   computed-flow ^{:line 86 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (net-flow acct-entries)
   initial-offset ^{:line 87 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (- ^{:line 87 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (account-balance account) computed-flow)]
  ^{:line 89 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (>= initial-offset 0)))

^{:line 91 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (defn last-entry-balance [entries acct-id]
  ^{:line 92 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (let [acct-entries ^{:line 92 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (entries-for-account entries acct-id)]
  ^{:line 93 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (if ^{:line 93 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (empty? acct-entries) 0 ^{:line 95 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (ledgerentry-balance-after ^{:line 95 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run1/ledger.rkt"} (last acct-entries)))))
