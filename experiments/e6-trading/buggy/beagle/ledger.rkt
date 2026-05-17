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

;; BUG-8: passes timestamp where ref-id expected
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
                   (timestamp-value ts)
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

;; BUG-29: passes trade to account-id accessor (wrong record type)
(defn record-withdrawal
  [(id : Long) (account : Account) (amt : Amount) (ts : Timestamp)] : LedgerEntry
  (let [new-bal (->Amount (- (amount-value (account-balance account))
                             (amount-value amt)))]
    (->LedgerEntry id
                   (account-balance account)
                   0
                   "withdrawal"
                   (->Amount (- 0 (amount-value amt)))
                   new-bal
                   ts)))

;; --- queries ---

;; BUG-9: compares account-id to ref-id field
(defn entries-for-account
  [(entries : (Vec LedgerEntry)) (acct-id : AccountId)] : (Vec LedgerEntry)
  (filterv
    (fn [(e : LedgerEntry)] : Boolean
      (= (ledgerentry-ref-id e)
         (accountid-value acct-id)))
    entries))

(defn entries-by-type
  [(entries : (Vec LedgerEntry)) (entry-type : String)] : (Vec LedgerEntry)
  (filterv
    (fn [(e : LedgerEntry)] : Boolean
      (= (ledgerentry-entry-type e) entry-type))
    entries))

;; BUG-10: reads balance-after instead of amount
(defn net-flow [(entries : (Vec LedgerEntry))] : Amount
  (let [amounts (mapv (fn [(e : LedgerEntry)] : Long
                        (amount-value (ledgerentry-balance-after e)))
                      entries)]
    (->Amount (reduce + 0 amounts))))

;; BUG-37: uses <= instead of >=
(defn reconcile [(entries : (Vec LedgerEntry)) (account : Account)] : Boolean
  (let [acct-entries (entries-for-account entries (account-id account))
        computed-flow (net-flow acct-entries)
        initial-offset (- (amount-value (account-balance account))
                          (amount-value computed-flow))]
    (<= initial-offset 0)))

(defn last-entry-balance [(entries : (Vec LedgerEntry)) (acct-id : AccountId)] : Amount
  (let [acct-entries (entries-for-account entries acct-id)]
    (if (empty? acct-entries)
      (->Amount 0)
      (ledgerentry-balance-after (last acct-entries)))))
