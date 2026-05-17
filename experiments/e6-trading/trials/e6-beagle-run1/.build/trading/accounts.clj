(ns trading.accounts)

;; AccountId : Long (scalar)

;; Amount : Long (scalar)

;; Timestamp : Long (scalar)

;; Email : String (scalar)

;; Currency : String (scalar)

^{:line 15 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defrecord Account [id email name balance currency created-at])

(defn account-id [r] (:id r))

(defn account-email [r] (:email r))

(defn account-name [r] (:name r))

(defn account-balance [r] (:balance r))

(defn account-currency [r] (:currency r))

(defn account-created-at [r] (:created-at r))

^{:line 25 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn create-account [id email name balance currency ts]
  ^{:line 28 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (->Account id email name balance currency ts))

^{:line 33 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn account-balance-amount [acct]
  ^{:line 34 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-balance acct))

^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn credit-account [acct amt]
  ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (let [current ^{:line 38 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-balance acct)
   new-bal ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (+ current amt)]
  ^{:line 40 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (assoc acct :balance new-bal)))

^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn debit-account [acct amt]
  ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (let [current ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-balance acct)
   new-bal ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (- current amt)]
  ^{:line 46 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (assoc acct :balance new-bal)))

^{:line 49 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn sufficient-balance? [acct amt]
  ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (>= ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-balance acct) amt))

^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn transfer [from to amt]
  ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (let [debited ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (debit-account from amt)
   credited ^{:line 55 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (credit-account to amt)]
  ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} [debited credited]))

^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn format-balance [acct]
  ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (let [bal ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-balance acct)
   curr ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-currency acct)
   dollars ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (quot bal 100)
   cents ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (mod ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (if ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (< bal 0) ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (- 0 bal) bal) 100)]
  ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (str curr " " dollars "." ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (if ^{:line 64 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (< cents 10) "0" "") cents)))

^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (defn account-age-days [acct now]
  ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (let [created ^{:line 68 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (account-created-at acct)
   current now
   diff-ms ^{:line 70 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (- current created)]
  ^{:line 71 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt"} (quot diff-ms 86400000)))
