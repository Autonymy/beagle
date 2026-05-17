(ns trading.ledger
  (:require [trading.accounts :refer :all :as acct]
            [trading.trades :refer :all :as trd]))

(defrecord LedgerEntry [id account-id ref-id entry-type amount balance-after timestamp])

(defn ledgerentry-id [r] (:id r))

(defn ledgerentry-account-id [r] (:account-id r))

(defn ledgerentry-ref-id [r] (:ref-id r))

(defn ledgerentry-entry-type [r] (:entry-type r))

(defn ledgerentry-amount [r] (:amount r))

(defn ledgerentry-balance-after [r] (:balance-after r))

(defn ledgerentry-timestamp [r] (:timestamp r))

;; BUG-8: fixed — uses trade-id as ref-id
(defn record-trade [id account trade ts]
  (let [net (trd/trade-net-amount trade)
   new-balance (if (< net 0) (- (account-balance account) (- 0 net)) (+ (account-balance account) net))]
  (->LedgerEntry id (account-id account) (trd/trade-id trade) "trade" net new-balance ts)))

;; BUG-23: fixed — returns a LedgerEntry
(defn record-deposit [id account amt ts]
  (let [new-bal (+ (account-balance account) amt)]
  (->LedgerEntry id (account-id account) 0 "deposit" amt new-bal ts)))

;; BUG-29: fixed — uses account-id
(defn record-withdrawal [id account amt ts]
  (let [new-bal (- (account-balance account) amt)]
  (->LedgerEntry id (account-id account) 0 "withdrawal" (- 0 amt) new-bal ts)))

;; BUG-9: fixed — compares account-id field
(defn entries-for-account [entries acct-id]
  (filterv (fn [e] (= (ledgerentry-account-id e) acct-id)) entries))

(defn entries-by-type [entries entry-type]
  (filterv (fn [e] (= (ledgerentry-entry-type e) entry-type)) entries))

;; BUG-10: fixed — reads amount field
(defn net-flow [entries]
  (let [amounts (mapv (fn [e] (ledgerentry-amount e)) entries)]
  (reduce + 0 amounts)))

;; BUG-37: fixed — uses >=
(defn reconcile [entries account]
  (let [acct-entries (entries-for-account entries (account-id account))
   computed-flow (net-flow acct-entries)
   initial-offset (- (account-balance account) computed-flow)]
  (>= initial-offset 0)))

(defn last-entry-balance [entries acct-id]
  (let [acct-entries (entries-for-account entries acct-id)]
  (if (empty? acct-entries) 0 (ledgerentry-balance-after (last acct-entries)))))
