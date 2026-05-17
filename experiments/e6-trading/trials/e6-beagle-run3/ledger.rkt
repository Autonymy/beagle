#lang beagle

(ns trading.ledger)

(require trading.accounts :as acct)
(require trading.trades :as trd)

;; --- records ---

(defrecord LedgerEntry
  [(id : Long)
   (account-id : AccountId)
   (ref-id : Long)
   (entry-type : String)
   (amount : Amount)
   (balance-after : Amount)
   (timestamp : Timestamp)])

;; --- constructors ---

;; BUG-8: fixed — uses trade-id as ref-id
(defn record-trade
  [(id : Long) (account : Account) (trade : Trade) (ts : Timestamp)] : LedgerEntry
  (let [net (trd/trade-net-amount trade)
        new-balance (if (< (amount-value net) 0)
                      (->Amount (- (amount-value (account-balance account))
                                   (- 0 (amount-value net))))
                      (->Amount (+ (amount-value (account-balance account))
                                   (amount-value net))))]
    (->LedgerEntry id
                   (account-id account)
                   (tradeid-value (trade-id trade))
                   "trade"
                   net
                   new-balance
                   ts)))

(defn record-deposit
  [(id : Long) (account : Account) (amt : Amount) (ts : Timestamp)] : LedgerEntry
  (let [new-bal (->Amount (+ (amount-value (account-balance account))
                             (amount-value amt)))]
    (->LedgerEntry id
                   (account-id account)
                   0
                   "deposit"
                   amt
                   new-bal
                   ts)))

;; BUG-29: fixed — uses account-id accessor
(defn record-withdrawal
  [(id : Long) (account : Account) (amt : Amount) (ts : Timestamp)] : LedgerEntry
  (let [new-bal (->Amount (- (amount-value (account-balance account))
                             (amount-value amt)))]
    (->LedgerEntry id
                   (account-id account)
                   0
                   "withdrawal"
                   (->Amount (- 0 (amount-value amt)))
                   new-bal
                   ts)))

;; --- queries ---

;; BUG-9: fixed — compares account-id field
(defn entries-for-account
  [(entries : (Vec LedgerEntry)) (acct-id : AccountId)] : (Vec LedgerEntry)
  (filterv
    (fn [(e : LedgerEntry)] : Boolean
      (= (accountid-value (ledgerentry-account-id e))
         (accountid-value acct-id)))
    entries))

(defn entries-by-type
  [(entries : (Vec LedgerEntry)) (entry-type : String)] : (Vec LedgerEntry)
  (filterv
    (fn [(e : LedgerEntry)] : Boolean
      (= (ledgerentry-entry-type e) entry-type))
    entries))

;; BUG-10: fixed — reads amount field
(defn net-flow [(entries : (Vec LedgerEntry))] : Amount
  (let [amounts (mapv (fn [(e : LedgerEntry)] : Long
                        (amount-value (ledgerentry-amount e)))
                      entries)]
    (->Amount (reduce + 0 amounts))))

;; BUG-37: fixed — uses >=
(defn reconcile [(entries : (Vec LedgerEntry)) (account : Account)] : Boolean
  (let [acct-entries (entries-for-account entries (account-id account))
        computed-flow (net-flow acct-entries)
        initial-offset (- (amount-value (account-balance account))
                          (amount-value computed-flow))]
    (>= initial-offset 0)))

(defn last-entry-balance [(entries : (Vec LedgerEntry)) (acct-id : AccountId)] : Amount
  (let [acct-entries (entries-for-account entries acct-id)]
    (if (empty? acct-entries)
      (->Amount 0)
      (ledgerentry-balance-after (last acct-entries)))))
