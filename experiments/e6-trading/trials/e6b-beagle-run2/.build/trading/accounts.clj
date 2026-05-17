(ns trading.accounts)

;; AccountId : Long (scalar)

;; Amount : Long (scalar)

;; Timestamp : Long (scalar)

;; Email : String (scalar)

;; Currency : String (scalar)

^{:line 15 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defrecord Account [id email name balance currency created-at])

(defn account-id [r] (:id r))

(defn account-email [r] (:email r))

(defn account-name [r] (:name r))

(defn account-balance [r] (:balance r))

(defn account-currency [r] (:currency r))

(defn account-created-at [r] (:created-at r))

^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn create-account [id email name balance currency ts]
  ^{:line 28 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (->Account id email name balance currency ts))

^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn account-balance-amount [acct]
  ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-balance acct))

^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn credit-account [acct amt]
  ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (let [current ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-balance acct)
   new-bal ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (+ current amt)]
  ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (assoc acct :balance new-bal)))

^{:line 41 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn debit-account [acct amt]
  ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (let [current ^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-balance acct)
   new-bal ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (- current amt)]
  ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (assoc acct :balance new-bal)))

^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn sufficient-balance? [acct amt]
  ^{:line 47 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (>= ^{:line 47 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-balance acct) amt))

^{:line 49 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn transfer [from to amt]
  ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (let [debited ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (debit-account from amt)
   credited ^{:line 51 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (credit-account to amt)]
  ^{:line 52 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} [debited credited]))

^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn format-balance [acct]
  ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (let [bal ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-balance acct)
   curr ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-currency acct)
   dollars ^{:line 57 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (quot bal 100)
   cents ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (mod ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (if ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (< bal 0) ^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (- 0 bal) bal) 100)]
  ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (str curr " " dollars "." ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (if ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (< cents 10) "0" "") cents)))

^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (defn account-age-days [acct now]
  ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (let [created ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (account-created-at acct)
   current now
   diff-ms ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (- current created)]
  ^{:line 65 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6b-beagle-run2/accounts.rkt"} (quot diff-ms 86400000)))
