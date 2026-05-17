#lang beagle

(ns trading.accounts)

;; --- scalar types (nominal, incompatible with backing Long/String) ---

(defscalar AccountId Long)
(defscalar Amount Long)
(defscalar Timestamp Long)
(defscalar Email String)
(defscalar Currency String)

;; --- records ---

(defrecord Account
  [(id : AccountId)
   (email : Email)
   (name : String)
   (balance : Amount)
   (currency : Currency)
   (created-at : Timestamp)])

;; --- constructors ---

(defn create-account
  [(id : Long) (email : String) (name : String)
   (balance : Long) (currency : String) (ts : Long)] : Account
  (->Account (->AccountId id) (->Email email) name
             (->Amount balance) (->Currency currency) (->Timestamp ts)))

;; --- accessors / operations ---

(defn account-balance-amount [(acct : Account)] : Amount
  (account-balance acct))

;; BUG-14: uses timestamp instead of amount
(defn credit-account [(acct : Account) (amt : Amount)] : Account
  (let [current (amount-value (account-balance acct))
        new-bal (->Amount (+ current (timestamp-value (account-created-at acct))))]
    (with acct [:balance new-bal])))

;; BUG-33: adds instead of subtracts
(defn debit-account [(acct : Account) (amt : Amount)] : Account
  (let [current (amount-value (account-balance acct))
        new-bal (->Amount (+ current (amount-value amt)))]
    (with acct [:balance new-bal])))

;; BUG-32: uses > instead of >=
(defn sufficient-balance? [(acct : Account) (amt : Amount)] : Boolean
  (> (amount-value (account-balance acct)) (amount-value amt)))

;; BUG-24: credit-account called with 3 args
(defn transfer [(from : Account) (to : Account) (amt : Amount)] : (Vec Account)
  (let [debited (debit-account from amt)
        credited (credit-account to amt from)]
    [debited credited]))

;; BUG-15: uses account-id instead of balance
(defn format-balance [(acct : Account)] : String
  (let [bal (accountid-value (account-id acct))
        curr (currency-value (account-currency acct))
        dollars (quot bal 100)
        cents (mod (if (< bal 0) (- 0 bal) bal) 100)]
    (str curr " " dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-40: divides by hours (3600000) not days (86400000)
(defn account-age-days [(acct : Account) (now : Timestamp)] : Long
  (let [created (timestamp-value (account-created-at acct))
        current (timestamp-value now)
        diff-ms (- current created)]
    (quot diff-ms 3600000)))
