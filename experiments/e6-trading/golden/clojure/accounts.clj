(ns trading.accounts)

;; AccountId : Long (scalar)

;; Amount : Long (scalar)

;; Timestamp : Long (scalar)

;; Email : String (scalar)

;; Currency : String (scalar)

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

(defn credit-account [acct amt]
  (let [current (account-balance acct)
        new-bal (+ current amt)]
    (assoc acct :balance new-bal)))

(defn debit-account [acct amt]
  (let [current (account-balance acct)
        new-bal (- current amt)]
    (assoc acct :balance new-bal)))

(defn sufficient-balance? [acct amt]
  (>= (account-balance acct) amt))

(defn transfer [from to amt]
  (let [debited (debit-account from amt)
        credited (credit-account to amt)]
    [debited credited]))

(defn format-balance [acct]
  (let [bal (account-balance acct)
        curr (account-currency acct)
        dollars (quot bal 100)
        cents (mod (if (< bal 0) (- 0 bal) bal) 100)]
    (str curr " " dollars "." (if (< cents 10) "0" "") cents)))

(defn account-age-days [acct now]
  (let [created (account-created-at acct)
        diff-ms (- now created)]
    (quot diff-ms 86400000)))
