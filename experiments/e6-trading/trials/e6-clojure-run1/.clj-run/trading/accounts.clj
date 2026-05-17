(ns trading.accounts)

(defrecord Account [id email name balance currency created-at])

(defn account-id [r] (:id r))

(defn account-email [r] (:email r))

(defn account-name [r] (:name r))

(defn account-balance [r] (:balance r))

(defn account-currency [r] (:currency r))

(defn account-created-at [r] (:created-at r))

(defn create-account [id email name balance currency ts]
  (->Account id email name balance currency ts))

(defn account-balance-amount [acct]
  (account-balance acct))

;; BUG-14 FIXED: uses amt parameter
(defn credit-account [acct amt]
  (let [current (account-balance acct)
   new-bal (+ current amt)]
  (assoc acct :balance new-bal)))

;; BUG-33 FIXED: subtracts
(defn debit-account [acct amt]
  (let [current (account-balance acct)
   new-bal (- current amt)]
  (assoc acct :balance new-bal)))

;; BUG-32 FIXED: uses >=
(defn sufficient-balance? [acct amt]
  (>= (account-balance acct) amt))

;; BUG-24 FIXED: credit-account called with correct 2 args
(defn transfer [from to amt]
  (let [debited (debit-account from amt)
   credited (credit-account to amt)]
  [debited credited]))

;; BUG-15 FIXED: uses account-balance
(defn format-balance [acct]
  (let [bal (account-balance acct)
   curr (account-currency acct)
   dollars (quot bal 100)
   cents (mod (if (< bal 0) (- 0 bal) bal) 100)]
  (str curr " " dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-40 FIXED: divides by 86400000 (days)
(defn account-age-days [acct now]
  (let [created (account-created-at acct)
   current now
   diff-ms (- current created)]
  (quot diff-ms 86400000)))
