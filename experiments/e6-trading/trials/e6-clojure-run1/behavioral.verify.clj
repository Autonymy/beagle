(ns verify.behavioral
  (:require [clojure.test :refer [deftest is run-tests testing]]))

;; ============================================================
;; E6 Trading System — Behavioral Verification
;;
;; Tests 40 injected bugs across 6 modules.
;; All scalars are erased: IDs, amounts, prices, quantities,
;; and timestamps are plain Longs. Amounts in cents.
;;
;; Usage:
;;   clj -Sdeps '{:paths ["<trial-dir>"]}' -M verify/behavioral.verify.clj
;; ============================================================

;; --- Module loading ---

(def ^:dynamic *loaded-modules* (atom #{}))

(doseq [[mod-key mod-sym]
        [[:accounts    'trading.accounts]
         [:instruments 'trading.instruments]
         [:orders      'trading.orders]
         [:trades      'trading.trades]
         [:ledger      'trading.ledger]
         [:reports     'trading.reports]]]
  (try
    (require mod-sym)
    (swap! *loaded-modules* conj mod-key)
    (catch Exception ex
      (println (str "LOAD FAILED: " (name mod-key) " — " (.getMessage ex))))))

(when (@*loaded-modules* :accounts)    (require '[trading.accounts :as acct]))
(when (@*loaded-modules* :instruments) (require '[trading.instruments :as inst]))
(when (@*loaded-modules* :orders)      (require '[trading.orders :as ord]))
(when (@*loaded-modules* :trades)      (require '[trading.trades :as trd]))
(when (@*loaded-modules* :ledger)      (require '[trading.ledger :as ldg]))
(when (@*loaded-modules* :reports)     (require '[trading.reports :as rpt]))

;; --- Test fixtures ---

(def acct1 (when (@*loaded-modules* :accounts)
             (acct/create-account 1 "alice@test.com" "Alice" 100000 "USD" 1000000000000)))

(def acct2 (when (@*loaded-modules* :accounts)
             (acct/create-account 2 "bob@test.com" "Bob" 50000 "USD" 1000000000000)))

(def inst1 (when (@*loaded-modules* :instruments)
             (inst/create-instrument 101 "AAPL" "Apple Inc" 15000 1 "USD")))

(def inst2 (when (@*loaded-modules* :instruments)
             (inst/create-instrument 102 "GOOG" "Alphabet" 280000 1 "USD")))

(def order1 (when (@*loaded-modules* :orders)
              (ord/create-order 1001 1 101 "buy" 10 15000 1000000100000)))

(def order2 (when (@*loaded-modules* :orders)
              (ord/create-order 1002 2 102 "sell" 5 280000 1000000200000)))

(def order3 (when (@*loaded-modules* :orders)
              (ord/create-order 1003 1 101 "buy" 5 14500 1000000300000)))

(def trade1 (when (and (@*loaded-modules* :trades) (@*loaded-modules* :orders))
              (trd/execute-trade 2001 order1 15000 1000000400000)))

(def trade2 (when (and (@*loaded-modules* :trades) (@*loaded-modules* :orders))
              (trd/execute-trade 2002 order2 280000 1000000500000)))

;; ============================================================
;; accounts module — bugs 14, 15, 32, 33, 40, 24
;; ============================================================

(deftest bug-14-credit-account-adds-amount-not-timestamp
  ;; Bug 14: credit-account adds timestamp to balance instead of amount
  (let [credited (acct/credit-account acct1 5000)]
    (is (= 105000 (:balance credited))
        "BUG-14: credit-account should add amount (5000) to balance"))
  (let [credited (acct/credit-account acct2 10000)]
    (is (= 60000 (:balance credited))
        "BUG-14: credit-account on acct2 should add 10000 to 50000"))
  (let [credited (acct/credit-account acct1 0)]
    (is (= 100000 (:balance credited))
        "BUG-14: credit-account with 0 should leave balance unchanged")))

(deftest bug-15-format-balance-uses-balance-not-id
  ;; Bug 15: format-balance uses account-id instead of balance for formatting
  (let [result (acct/format-balance acct1)]
    (is (= "USD 1000.00" result)
        "BUG-15: format-balance should display $1000.00 for 100000 cents"))
  (let [result (acct/format-balance acct2)]
    (is (= "USD 500.00" result)
        "BUG-15: format-balance should display $500.00 for 50000 cents")))

(deftest bug-32-sufficient-balance-uses-gte
  ;; Bug 32: sufficient-balance? uses > instead of >=
  (is (true? (acct/sufficient-balance? acct1 100000))
      "BUG-32: sufficient-balance? should return true when balance == amount")
  (is (true? (acct/sufficient-balance? acct1 50000))
      "BUG-32: sufficient-balance? should return true when balance > amount")
  (is (false? (acct/sufficient-balance? acct1 100001))
      "BUG-32: sufficient-balance? should return false when balance < amount"))

(deftest bug-33-debit-account-subtracts
  ;; Bug 33: debit-account adds instead of subtracts
  (let [debited (acct/debit-account acct1 30000)]
    (is (= 70000 (:balance debited))
        "BUG-33: debit-account should subtract amount from balance"))
  (let [debited (acct/debit-account acct2 50000)]
    (is (= 0 (:balance debited))
        "BUG-33: debit-account should reach zero when debiting full balance"))
  (let [debited (acct/debit-account acct1 100000)]
    (is (= 0 (:balance debited))
        "BUG-33: debit-account full balance debit")))

(deftest bug-40-account-age-days-divides-by-86400000
  ;; Bug 40: account-age-days divides by 3600000 (hours) not 86400000 (days)
  (let [now (+ 1000000000000 (* 10 86400000))  ;; 10 days after creation
        age (acct/account-age-days acct1 now)]
    (is (= 10 age)
        "BUG-40: account-age-days should return 10 for 10 days elapsed"))
  (let [now (+ 1000000000000 (* 1 86400000))  ;; 1 day after creation
        age (acct/account-age-days acct1 now)]
    (is (= 1 age)
        "BUG-40: account-age-days should return 1 for 1 day elapsed"))
  (let [now 1000000000000  ;; same as created-at
        age (acct/account-age-days acct1 now)]
    (is (= 0 age)
        "BUG-40: account-age-days should return 0 when no time elapsed")))

(deftest bug-24-transfer-correct-arity
  ;; Bug 24: transfer calls credit-account with 3 args (wrong arity)
  (let [[debited credited] (acct/transfer acct1 acct2 20000)]
    (is (= 80000 (:balance debited))
        "BUG-24: transfer should debit from-account")
    (is (= 70000 (:balance credited))
        "BUG-24: transfer should credit to-account")))

;; ============================================================
;; instruments module — bugs 16, 17, 25, 39
;; ============================================================

(deftest bug-16-instrument-value-multiplies-price-by-qty
  ;; Bug 16: instrument-value returns price (not price*qty)
  (let [val (inst/instrument-value inst1 10)]
    (is (= 150000 val)
        "BUG-16: instrument-value should be price*qty = 15000*10"))
  (let [val (inst/instrument-value inst2 3)]
    (is (= 840000 val)
        "BUG-16: instrument-value for GOOG 3 shares = 280000*3"))
  (let [val (inst/instrument-value inst1 1)]
    (is (= 15000 val)
        "BUG-16: instrument-value for 1 share = price")))

(deftest bug-17-instrument-total-cost-uses-fee-bps-not-lot-size
  ;; Bug 17: instrument-total-cost uses lot-size as fee-bps
  ;; For inst1: base = 15000*10 = 150000, fee at 50 bps = 150000*50/10000 = 750
  (let [cost (inst/instrument-total-cost inst1 10 50)]
    (is (= 150750 cost)
        "BUG-17: instrument-total-cost should use fee-bps param, not lot-size"))
  ;; At 0 bps fee, cost = base
  (let [cost (inst/instrument-total-cost inst1 10 0)]
    (is (= 150000 cost)
        "BUG-17: instrument-total-cost at 0 bps should equal base cost"))
  ;; At 100 bps (1%): base=15000*10=150000, fee=1500
  (let [cost (inst/instrument-total-cost inst1 10 100)]
    (is (= 151500 cost)
        "BUG-17: instrument-total-cost at 100 bps should add 1%")))

(deftest bug-25-update-price-uses-price-field
  ;; Bug 25: update-price uses :ticker field on wrong record
  (let [updated (inst/update-price inst1 16000)]
    (is (= 16000 (:price updated))
        "BUG-25: update-price should set :price field")
    (is (= "AAPL" (:ticker updated))
        "BUG-25: update-price should not modify :ticker")
    (is (= "Apple Inc" (:name updated))
        "BUG-25: update-price should not modify :name")
    (is (= 101 (:id updated))
        "BUG-25: update-price should not modify :id")))

(deftest bug-39-price-change-divides-by-old-price
  ;; Bug 39: price-change divides by new price not old
  ;; ((16000 - 15000) * 10000) / 15000 = 666 bps
  (let [change (inst/price-change 15000 16000)]
    (is (= 666 change)
        "BUG-39: price-change should divide by old price"))
  ;; Reverse: ((15000 - 16000) * 10000) / 16000 = -625 bps
  (let [change (inst/price-change 16000 15000)]
    (is (= -625 change)
        "BUG-39: price-change should divide by old price (decrease case)"))
  ;; No change: 0 bps
  (let [change (inst/price-change 15000 15000)]
    (is (= 0 change)
        "BUG-39: price-change should be 0 when prices are equal")))

;; ============================================================
;; orders module — bugs 1, 2, 3, 18, 20, 27, 35, 36
;; ============================================================

(deftest bug-01-create-order-correct-field-assignment
  ;; Bug 1: create-order passes instrument-id where account-id expected (swapped)
  (let [o (ord/create-order 1001 1 101 "buy" 10 15000 1000000100000)]
    (is (= 1 (:account-id o))
        "BUG-01: create-order should put account-id in account-id field")
    (is (= 101 (:instrument-id o))
        "BUG-01: create-order should put instrument-id in instrument-id field")
    (is (= "pending" (:status o))
        "BUG-01: create-order should set status to pending")
    (is (nil? (:filled-at o))
        "BUG-01: create-order should set filled-at to nil")))

(deftest bug-02-order-total-multiplies-price-by-qty
  ;; Bug 2: order-total returns price directly instead of price*qty
  (let [total (ord/order-total order1)]
    (is (= 150000 total)
        "BUG-02: order-total should be limit-price*quantity = 15000*10"))
  (let [total (ord/order-total order2)]
    (is (= 1400000 total)
        "BUG-02: order-total for order2 = 280000*5"))
  (let [total (ord/order-total order3)]
    (is (= 72500 total)
        "BUG-02: order-total for order3 = 14500*5")))

(deftest bug-03-validate-order-uses-correct-amount
  ;; Bug 3: validate-order uses order-id value in balance check
  ;; acct1 has 100000 balance, order1 total is 150000 → insufficient
  (let [result (ord/validate-order order1 acct1 inst1)]
    (is (= "insufficient balance" result)
        "BUG-03: validate-order should check order-total against balance"))
  ;; Small order that fits in balance: 5 * 15000 = 75000 <= 100000
  (let [small-order (ord/create-order 1010 1 101 "buy" 5 15000 1000000100000)
        result (ord/validate-order small-order acct1 inst1)]
    (is (nil? result)
        "BUG-03: validate-order should return nil when balance is sufficient")))

(deftest bug-18-total-exposure-sums-order-totals
  ;; Bug 18: total-exposure sums order-ids instead of order totals
  (let [orders [order1 order3]
        exposure (ord/total-exposure orders)]
    ;; order1: 15000*10=150000, order3: 14500*5=72500, sum=222500
    (is (= 222500 exposure)
        "BUG-18: total-exposure should sum order totals of pending orders"))
  ;; With a cancelled order mixed in — only pending should count
  (let [cancelled (ord/cancel-order order2 "test")
        orders [order1 cancelled order3]
        exposure (ord/total-exposure orders)]
    (is (= 222500 exposure)
        "BUG-18: total-exposure should ignore cancelled orders")))

(deftest bug-20-orders-for-account-uses-account-id
  ;; Bug 20: accesses order-instrument-id as order-account-id in filter
  (let [orders [order1 order2 order3]
        alice-orders (ord/orders-for-account orders 1)]
    (is (= 2 (count alice-orders))
        "BUG-20: orders-for-account should filter by account-id field"))
  (let [orders [order1 order2 order3]
        bob-orders (ord/orders-for-account orders 2)]
    (is (= 1 (count bob-orders))
        "BUG-20: orders-for-account should find bob's single order")))

(deftest bug-27-fill-order-sets-filled-at
  ;; Bug 27: fill-order doesn't set :filled-at field
  (let [filled (ord/fill-order order1 1000000500000)]
    (is (= "filled" (:status filled))
        "BUG-27: fill-order should set status to filled")
    (is (= 1000000500000 (:filled-at filled))
        "BUG-27: fill-order should set filled-at timestamp")))

(deftest bug-35-validate-order-checks-buy-side
  ;; Bug 35: validate-order missing (= side "buy") check
  ;; Sell orders should NOT check balance
  (let [sell-order (ord/create-order 1004 2 101 "sell" 10 15000 1000000100000)
        ;; acct2 has 50000, order total would be 150000 > balance
        ;; But sell orders shouldn't check balance
        result (ord/validate-order sell-order acct2 inst1)]
    (is (nil? result)
        "BUG-35: validate-order should not check balance for sell orders")))

(deftest bug-36-pending-orders-filters-correctly
  ;; Bug 36: pending-orders returns cancelled orders too
  (let [cancelled (ord/cancel-order order2 "user request")
        orders [order1 cancelled order3]
        pending (ord/pending-orders orders)]
    (is (= 2 (count pending))
        "BUG-36: pending-orders should not include cancelled orders")
    (is (every? #(= "pending" (:status %)) pending)
        "BUG-36: all returned orders should have pending status")))

;; ============================================================
;; trades module — bugs 4, 5, 6, 7, 19, 22, 28, 31, 34
;; ============================================================

(deftest bug-04-execute-trade-uses-correct-trade-id
  ;; Bug 4: execute-trade uses order-id value as trade-id
  (let [t (trd/execute-trade 2001 order1 15000 1000000400000)]
    (is (= 2001 (:id t))
        "BUG-04: execute-trade should use the provided trade-id")
    (is (= 1001 (:order-id t))
        "BUG-04: execute-trade should store order's id as order-id")))

(deftest bug-05-trade-pnl-uses-quantity-correctly
  ;; Bug 5: trade-pnl uses exec-price where quantity expected
  ;; trade1: buy 10 @ 15000, current = 16000 → pnl = (16000-15000)*10 = 10000
  (let [pnl (trd/trade-pnl trade1 16000)]
    (is (= 10000 pnl)
        "BUG-05: trade-pnl should multiply price-diff by quantity"))
  ;; No change: pnl = 0
  (let [pnl (trd/trade-pnl trade1 15000)]
    (is (= 0 pnl)
        "BUG-05: trade-pnl should be 0 when current = exec"))
  ;; Loss: current < exec for buy
  (let [pnl (trd/trade-pnl trade1 14000)]
    (is (= -10000 pnl)
        "BUG-05: trade-pnl negative for buy when current < exec")))

(deftest bug-06-settle-trade-uses-account-id
  ;; Bug 6: settle-trade uses trade-id to look up account
  ;; trade1 is a buy: net = -(total+fee). total=150000, fee=150000*10/10000=150
  ;; settle should debit acct1 by (total+fee) = 150150
  (let [settled (trd/settle-trade trade1 acct1)]
    (is (= (- 100000 150150) (:balance settled))
        "BUG-06: settle-trade should adjust account balance by net amount"))
  ;; trade2 is a sell: net = total-fee = 1400000-1400 = 1398600 (credit)
  (let [settled (trd/settle-trade trade2 acct2)]
    (is (= (+ 50000 1398600) (:balance settled))
        "BUG-06: settle-trade for sell should credit account")))

(deftest bug-07-trade-net-amount-returns-net
  ;; Bug 7: trade-net-amount returns fee value instead of net
  ;; trade1: buy, total=150000, fee=150 → net = -(150000+150) = -150150
  (let [net (trd/trade-net-amount trade1)]
    (is (= -150150 net)
        "BUG-07: trade-net-amount for buy should be -(total+fee)"))
  ;; trade2: sell, total=1400000, fee=1400 → net = 1400000-1400 = 1398600
  (let [net (trd/trade-net-amount trade2)]
    (is (= 1398600 net)
        "BUG-07: trade-net-amount for sell should be total-fee")))

(deftest bug-19-execute-trade-requires-4-args
  ;; Bug 19: execute-trade called with 3 args (missing timestamp)
  ;; This tests that execute-trade works correctly with all 4 args
  (let [t (trd/execute-trade 2003 order1 14800 1000000600000)]
    (is (= 1000000600000 (:executed-at t))
        "BUG-19: execute-trade should accept and store timestamp (4th arg)")
    (is (= 14800 (:exec-price t))
        "BUG-19: execute-trade should store exec-price")
    (is (= 10 (:quantity t))
        "BUG-19: execute-trade should pull quantity from order")))

(deftest bug-22-trades-for-account-uses-account-id
  ;; Bug 22: trades-for-account uses trade-instrument-id instead of trade-account-id
  (let [trades [trade1 trade2]
        alice-trades (trd/trades-for-account trades 1)]
    (is (= 1 (count alice-trades))
        "BUG-22: trades-for-account should filter by account-id"))
  (let [trades [trade1 trade2]
        bob-trades (trd/trades-for-account trades 2)]
    (is (= 1 (count bob-trades))
        "BUG-22: trades-for-account should find bob's trade")))

(deftest bug-28-avg-exec-price-uses-quantity
  ;; Bug 28: avg-exec-price accesses quantity from price variable
  ;; Two buy trades: trade1 (10 @ 15000), another (5 @ 14800)
  ;; avg = (15000*10 + 14800*5) / (10+5) = (150000+74000)/15 = 14933
  (let [trade3 (trd/execute-trade 2003 order3 14800 1000000600000)
        avg (trd/avg-exec-price [trade1 trade3])]
    (is (= 14933 avg)
        "BUG-28: avg-exec-price should weight by quantity")))

(deftest bug-31-trade-fee-is-10-bps
  ;; Bug 31: trade-fee uses wrong basis points
  ;; trade1: total = 15000*10 = 150000, fee = 150000*10/10000 = 150
  (let [fee (:fee trade1)]
    (is (= 150 fee)
        "BUG-31: trade fee should be 10 bps (0.1%) of total"))
  ;; trade2: total = 280000*5 = 1400000, fee = 1400000*10/10000 = 1400
  (let [fee (:fee trade2)]
    (is (= 1400 fee)
        "BUG-31: trade2 fee should be 10 bps of 1400000")))

(deftest bug-34-trade-pnl-flips-sign-for-sell
  ;; Bug 34: trade-pnl doesn't flip sign for sell orders
  ;; trade2: sell 5 @ 280000, current = 270000 → pnl = (280000-270000)*5 = 50000
  (let [pnl (trd/trade-pnl trade2 270000)]
    (is (= 50000 pnl)
        "BUG-34: trade-pnl for sell should be (exec-current)*qty"))
  ;; sell at loss: current > exec → negative pnl
  (let [pnl (trd/trade-pnl trade2 290000)]
    (is (= -50000 pnl)
        "BUG-34: trade-pnl for sell should be negative when current > exec")))

;; ============================================================
;; ledger module — bugs 8, 9, 10, 23, 29, 37
;; ============================================================

(deftest bug-08-record-trade-uses-ref-id-correctly
  ;; Bug 8: record-trade passes timestamp where ref-id expected
  (let [entry (ldg/record-trade 5001 acct1 trade1 1000000700000)]
    (is (= (:id trade1) (:ref-id entry))
        "BUG-08: record-trade should use trade-id as ref-id")
    (is (= "trade" (:entry-type entry))
        "BUG-08: record-trade entry-type should be 'trade'")
    (is (= 1000000700000 (:timestamp entry))
        "BUG-08: record-trade should store timestamp in timestamp field")
    (is (= 1 (:account-id entry))
        "BUG-08: record-trade should use account's id")))

(deftest bug-09-entries-for-account-compares-account-id
  ;; Bug 9: entries-for-account compares account-id to ref-id field
  (let [e1 (ldg/record-trade 5001 acct1 trade1 1000000700000)
        e2 (ldg/record-trade 5002 acct2 trade2 1000000800000)
        entries [e1 e2]
        alice-entries (ldg/entries-for-account entries 1)]
    (is (= 1 (count alice-entries))
        "BUG-09: entries-for-account should match on account-id field"))
  (let [e1 (ldg/record-trade 5001 acct1 trade1 1000000700000)
        e2 (ldg/record-trade 5002 acct2 trade2 1000000800000)
        entries [e1 e2]
        bob-entries (ldg/entries-for-account entries 2)]
    (is (= 1 (count bob-entries))
        "BUG-09: entries-for-account should find bob's entries too")))

(deftest bug-10-net-flow-reads-amount-field
  ;; Bug 10: net-flow reads balance-after instead of amount field
  (let [e1 (ldg/record-deposit 5003 acct1 20000 1000000900000)
        e2 (ldg/record-withdrawal 5004 acct1 5000 1000001000000)
        entries [e1 e2]
        flow (ldg/net-flow entries)]
    ;; deposit = +20000, withdrawal amount stored as -5000 → net = 15000
    (is (= 15000 flow)
        "BUG-10: net-flow should sum the amount field, not balance-after"))
  ;; Single deposit
  (let [e1 (ldg/record-deposit 5005 acct1 30000 1000000900000)
        flow (ldg/net-flow [e1])]
    (is (= 30000 flow)
        "BUG-10: net-flow of single deposit should equal deposit amount")))

(deftest bug-23-record-deposit-returns-ledger-entry
  ;; Bug 23: record-deposit returns String annotation but builds LedgerEntry
  (let [entry (ldg/record-deposit 5003 acct1 20000 1000000900000)]
    (is (= 5003 (:id entry))
        "BUG-23: record-deposit should return a LedgerEntry")
    (is (= 1 (:account-id entry))
        "BUG-23: record-deposit entry should have correct account-id")
    (is (= "deposit" (:entry-type entry))
        "BUG-23: record-deposit entry-type should be 'deposit'")
    (is (= 120000 (:balance-after entry))
        "BUG-23: record-deposit balance-after should be old + deposit amount")))

(deftest bug-29-record-withdrawal-uses-correct-record
  ;; Bug 29: record-withdrawal passes wrong record to account-id accessor
  (let [entry (ldg/record-withdrawal 5004 acct1 5000 1000001000000)]
    (is (= 1 (:account-id entry))
        "BUG-29: record-withdrawal should use account's id")
    (is (= "withdrawal" (:entry-type entry))
        "BUG-29: record-withdrawal entry-type should be 'withdrawal'")
    (is (= 95000 (:balance-after entry))
        "BUG-29: record-withdrawal balance-after should be 100000-5000"))
  (let [entry (ldg/record-withdrawal 5005 acct2 10000 1000001000000)]
    (is (= 2 (:account-id entry))
        "BUG-29: record-withdrawal for acct2 should use account 2's id")))

(deftest bug-37-reconcile-uses-gte
  ;; Bug 37: reconcile uses <= instead of >=
  ;; acct1: balance=100000. If entries show net-flow=100000, initial=0 → 0>=0 true
  (let [deposit-entry (ldg/record-deposit 5005 acct1 100000 1000000000000)
        entries [deposit-entry]]
    (is (true? (ldg/reconcile entries acct1))
        "BUG-37: reconcile should return true when initial-offset >= 0"))
  ;; With no entries, net-flow=0, initial = balance - 0 = 100000 >= 0 → true
  (is (true? (ldg/reconcile [] acct1))
      "BUG-37: reconcile with empty entries should return true"))

;; ============================================================
;; reports module — bugs 11, 12, 13, 21, 26, 30, 38
;; ============================================================

(deftest bug-11-calculate-position-uses-quantity
  ;; Bug 11: calculate-position uses instrument-id value as quantity
  ;; trade1: buy 10 AAPL, no sells → net_qty=10, value=15000*10=150000
  (let [pos (rpt/calculate-position [trade1] 101 15000 "AAPL")]
    (is (= 10 (:quantity pos))
        "BUG-11: calculate-position should compute net quantity from trades")
    (is (= 150000 (:current-value pos))
        "BUG-11: position current-value should be current-price * net-qty")
    (is (= "AAPL" (:ticker pos))
        "BUG-11: position should carry the ticker")
    (is (= 101 (:instrument-id pos))
        "BUG-11: position should carry instrument-id")))

(deftest bug-12-portfolio-value-sums-current-value
  ;; Bug 12: portfolio-value accumulates avg-price instead of current-value
  (let [pos1 (rpt/calculate-position [trade1] 101 16000 "AAPL")
        ;; pos1: net_qty = 10-0 = 10, value = 16000*10 = 160000
        total-single (rpt/portfolio-value [pos1])]
    (is (= 160000 total-single)
        "BUG-12: portfolio-value of single position"))
  (let [pos1 (rpt/calculate-position [trade1] 101 16000 "AAPL")
        pos2 (rpt/calculate-position [trade2] 102 275000 "GOOG")
        ;; pos2: net_qty = 0-5 = -5, value = 275000*(-5) = -1375000
        total (rpt/portfolio-value [pos1 pos2])]
    (is (= (+ 160000 -1375000) total)
        "BUG-12: portfolio-value should sum current-value fields")))

(deftest bug-13-total-pnl-returns-amount-sum
  ;; Bug 13: total-pnl returns price-sum instead of amount-sum
  ;; trade1: buy 10@15000, current=16000, pnl=(16000-15000)*10=10000
  ;; trade2: sell 5@280000, current=270000, pnl=(280000-270000)*5=50000
  (let [pnl (rpt/total-pnl [trade1 trade2] [16000 270000] [101 102])]
    (is (= 60000 pnl)
        "BUG-13: total-pnl should return sum of trade pnl amounts"))
  ;; Single instrument
  (let [pnl (rpt/total-pnl [trade1] [16000] [101])]
    (is (= 10000 pnl)
        "BUG-13: total-pnl for single trade should equal that trade's pnl")))

(deftest bug-21-format-pnl-report-calls-defined-fn
  ;; Bug 21: format-pnl-report calls undefined calculate-pnl
  (let [report (rpt/generate-pnl-report 1 [trade1] [16000] [101] 1000001000000)
        formatted (rpt/format-pnl-report report)]
    (is (string? formatted)
        "BUG-21: format-pnl-report should return a string")
    (is (.contains formatted "1 trades")
        "BUG-21: format-pnl-report should include trade count")))

(deftest bug-26-account-summary-calls-format-balance-correctly
  ;; Bug 26: account-summary calls format-balance with wrong arg count
  (let [entries [(ldg/record-trade 5001 acct1 trade1 1000000700000)]
        summary (rpt/account-summary acct1 [trade1] entries)]
    (is (string? summary)
        "BUG-26: account-summary should return a string")
    (is (.contains summary "Alice")
        "BUG-26: account-summary should include account name")
    (is (.contains summary "1 trades")
        "BUG-26: account-summary should include trade count")
    (is (.contains summary "1 ledger entries")
        "BUG-26: account-summary should include entry count")
    (is (.contains summary "USD")
        "BUG-26: account-summary should include formatted balance")))

(deftest bug-30-generate-pnl-report-uses-timestamp
  ;; Bug 30: generate-pnl-report passes String where Timestamp expected
  (let [report (rpt/generate-pnl-report 1 [trade1] [16000] [101] 1000001000000)]
    (is (= 1000001000000 (:as-of report))
        "BUG-30: generate-pnl-report as-of should be the provided timestamp")
    (is (= 1 (:account-id report))
        "BUG-30: generate-pnl-report should set correct account-id")
    (is (= 1 (:num-trades report))
        "BUG-30: generate-pnl-report should count trades")
    (is (integer? (:as-of report))
        "BUG-30: as-of should be a Long timestamp")))

(deftest bug-38-calculate-position-counts-sells-correctly
  ;; Bug 38: calculate-position counts sell qty as buy qty
  ;; Buy 10 then sell 3 → net = 7
  (let [sell-order (ord/create-order 1005 1 101 "sell" 3 15500 1000000700000)
        sell-trade (trd/execute-trade 2004 sell-order 15500 1000000800000)
        pos (rpt/calculate-position [trade1 sell-trade] 101 16000 "AAPL")]
    (is (= 7 (:quantity pos))
        "BUG-38: calculate-position should subtract sell qty from buy qty")
    (is (= (* 16000 7) (:current-value pos))
        "BUG-38: current-value should use net quantity"))
  ;; Sell all: net = 0
  (let [sell-order (ord/create-order 1006 1 101 "sell" 10 15500 1000000700000)
        sell-trade (trd/execute-trade 2005 sell-order 15500 1000000800000)
        pos (rpt/calculate-position [trade1 sell-trade] 101 16000 "AAPL")]
    (is (= 0 (:quantity pos))
        "BUG-38: selling all should result in 0 net quantity")))

;; ============================================================
;; Run tests and report score
;; ============================================================

(defn -main []
  (let [results (run-tests 'verify.behavioral)
        pass (:pass results)
        fail (:fail results)
        error (:error results)
        total-fail (+ fail error)
        total (+ pass total-fail)]
    (println)
    (println (str pass "/" total " assertions passed"))))

(-main)
