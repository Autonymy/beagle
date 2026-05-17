(ns verify.behavioral-stripped
  (:require [clojure.test :refer [deftest is run-tests testing]]))

;; ============================================================
;; E6 Trading System — Behavioral Verification
;;
;; Behavioral verification for trading system.
;; All scalars are erased: IDs, amounts, prices, quantities,
;; and timestamps are plain Longs. Amounts in cents.
;;
;; Usage:
;;   clj -Sdeps '{:paths ["<trial-dir>"]}' -M verify/behavioral-stripped.verify.clj
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
;; accounts module
;; ============================================================

(deftest test-credit-account
  ;; credit-account should increase balance by the given amount
  (let [credited (acct/credit-account acct1 5000)]
    (is (= 105000 (:balance credited))
        "credit-account should increase balance by the given amount"))
  (let [credited (acct/credit-account acct2 10000)]
    (is (= 60000 (:balance credited))
        "credit-account should add the specified amount to the existing balance"))
  (let [credited (acct/credit-account acct1 0)]
    (is (= 100000 (:balance credited))
        "credit-account with zero should leave balance unchanged")))

(deftest test-format-balance
  ;; format-balance should display the account balance in currency format
  (let [result (acct/format-balance acct1)]
    (is (= "USD 1000.00" result)
        "format-balance should display the balance in currency format"))
  (let [result (acct/format-balance acct2)]
    (is (= "USD 500.00" result)
        "format-balance should format the correct balance amount")))

(deftest test-sufficient-balance
  ;; sufficient-balance? should return true when balance covers the amount
  (is (true? (acct/sufficient-balance? acct1 100000))
      "sufficient-balance? should return true when balance equals amount")
  (is (true? (acct/sufficient-balance? acct1 50000))
      "sufficient-balance? should return true when balance exceeds amount")
  (is (false? (acct/sufficient-balance? acct1 100001))
      "sufficient-balance? should return false when balance is less than amount"))

(deftest test-debit-account
  ;; debit-account should decrease balance by the given amount
  (let [debited (acct/debit-account acct1 30000)]
    (is (= 70000 (:balance debited))
        "debit-account should subtract amount from balance"))
  (let [debited (acct/debit-account acct2 50000)]
    (is (= 0 (:balance debited))
        "debit-account should reach zero when debiting full balance"))
  (let [debited (acct/debit-account acct1 100000)]
    (is (= 0 (:balance debited))
        "debit-account should handle full balance debit")))

(deftest test-account-age-days
  ;; account-age-days should compute elapsed days since account creation
  (let [now (+ 1000000000000 (* 10 86400000))  ;; 10 days after creation
        age (acct/account-age-days acct1 now)]
    (is (= 10 age)
        "account-age-days should return correct number of elapsed days"))
  (let [now (+ 1000000000000 (* 1 86400000))  ;; 1 day after creation
        age (acct/account-age-days acct1 now)]
    (is (= 1 age)
        "account-age-days should return 1 for one day elapsed"))
  (let [now 1000000000000  ;; same as created-at
        age (acct/account-age-days acct1 now)]
    (is (= 0 age)
        "account-age-days should return 0 when no time has elapsed")))

(deftest test-transfer
  ;; transfer should debit the source and credit the destination
  (let [[debited credited] (acct/transfer acct1 acct2 20000)]
    (is (= 80000 (:balance debited))
        "transfer should debit from-account")
    (is (= 70000 (:balance credited))
        "transfer should credit to-account")))

;; ============================================================
;; instruments module
;; ============================================================

(deftest test-instrument-value
  ;; instrument-value should return price multiplied by quantity
  (let [val (inst/instrument-value inst1 10)]
    (is (= 150000 val)
        "instrument-value should multiply price by quantity"))
  (let [val (inst/instrument-value inst2 3)]
    (is (= 840000 val)
        "instrument-value should compute correctly for any instrument"))
  (let [val (inst/instrument-value inst1 1)]
    (is (= 15000 val)
        "instrument-value for single unit should equal price")))

(deftest test-instrument-total-cost
  ;; instrument-total-cost should add fee (in basis points) to the base cost
  ;; For inst1: base = 15000*10 = 150000, fee at 50 bps = 150000*50/10000 = 750
  (let [cost (inst/instrument-total-cost inst1 10 50)]
    (is (= 150750 cost)
        "instrument-total-cost should apply the given fee in basis points"))
  ;; At 0 bps fee, cost = base
  (let [cost (inst/instrument-total-cost inst1 10 0)]
    (is (= 150000 cost)
        "instrument-total-cost at zero bps should equal base cost"))
  ;; At 100 bps (1%): base=15000*10=150000, fee=1500
  (let [cost (inst/instrument-total-cost inst1 10 100)]
    (is (= 151500 cost)
        "instrument-total-cost at 100 bps should add 1% fee")))

(deftest test-update-price
  ;; update-price should set the new price without modifying other fields
  (let [updated (inst/update-price inst1 16000)]
    (is (= 16000 (:price updated))
        "update-price should set the new price")
    (is (= "AAPL" (:ticker updated))
        "update-price should not modify ticker")
    (is (= "Apple Inc" (:name updated))
        "update-price should not modify name")
    (is (= 101 (:id updated))
        "update-price should not modify id")))

(deftest test-price-change
  ;; price-change should compute the change in basis points relative to old price
  ;; ((16000 - 15000) * 10000) / 15000 = 666 bps
  (let [change (inst/price-change 15000 16000)]
    (is (= 666 change)
        "price-change should compute basis points relative to old price"))
  ;; Reverse: ((15000 - 16000) * 10000) / 16000 = -625 bps
  (let [change (inst/price-change 16000 15000)]
    (is (= -625 change)
        "price-change should handle price decreases"))
  ;; No change: 0 bps
  (let [change (inst/price-change 15000 15000)]
    (is (= 0 change)
        "price-change should be zero when prices are equal")))

;; ============================================================
;; orders module
;; ============================================================

(deftest test-create-order-fields
  ;; create-order should assign all fields to the correct positions
  (let [o (ord/create-order 1001 1 101 "buy" 10 15000 1000000100000)]
    (is (= 1 (:account-id o))
        "create-order should assign account-id correctly")
    (is (= 101 (:instrument-id o))
        "create-order should assign instrument-id correctly")
    (is (= "pending" (:status o))
        "create-order should set status to pending")
    (is (nil? (:filled-at o))
        "create-order should set filled-at to nil")))

(deftest test-order-total
  ;; order-total should return limit-price multiplied by quantity
  (let [total (ord/order-total order1)]
    (is (= 150000 total)
        "order-total should multiply limit-price by quantity"))
  (let [total (ord/order-total order2)]
    (is (= 1400000 total)
        "order-total should compute correctly for any order"))
  (let [total (ord/order-total order3)]
    (is (= 72500 total)
        "order-total should handle different price/quantity combinations")))

(deftest test-validate-order-balance
  ;; validate-order should check order total against account balance
  ;; acct1 has 100000 balance, order1 total is 150000 -> insufficient
  (let [result (ord/validate-order order1 acct1 inst1)]
    (is (= "insufficient balance" result)
        "validate-order should reject when order total exceeds balance"))
  ;; Small order that fits in balance: 5 * 15000 = 75000 <= 100000
  (let [small-order (ord/create-order 1010 1 101 "buy" 5 15000 1000000100000)
        result (ord/validate-order small-order acct1 inst1)]
    (is (nil? result)
        "validate-order should return nil when balance is sufficient")))

(deftest test-total-exposure
  ;; total-exposure should sum order totals for pending orders
  (let [orders [order1 order3]
        exposure (ord/total-exposure orders)]
    ;; order1: 15000*10=150000, order3: 14500*5=72500, sum=222500
    (is (= 222500 exposure)
        "total-exposure should sum order totals of pending orders"))
  ;; With a cancelled order mixed in — only pending should count
  (let [cancelled (ord/cancel-order order2 "test")
        orders [order1 cancelled order3]
        exposure (ord/total-exposure orders)]
    (is (= 222500 exposure)
        "total-exposure should ignore cancelled orders")))

(deftest test-orders-for-account
  ;; orders-for-account should filter orders by account-id
  (let [orders [order1 order2 order3]
        alice-orders (ord/orders-for-account orders 1)]
    (is (= 2 (count alice-orders))
        "orders-for-account should return all orders for the given account"))
  (let [orders [order1 order2 order3]
        bob-orders (ord/orders-for-account orders 2)]
    (is (= 1 (count bob-orders))
        "orders-for-account should find the correct subset")))

(deftest test-fill-order
  ;; fill-order should mark the order as filled with a timestamp
  (let [filled (ord/fill-order order1 1000000500000)]
    (is (= "filled" (:status filled))
        "fill-order should set status to filled")
    (is (= 1000000500000 (:filled-at filled))
        "fill-order should set the filled-at timestamp")))

(deftest test-validate-order-sell
  ;; validate-order should not check balance for sell orders
  (let [sell-order (ord/create-order 1004 2 101 "sell" 10 15000 1000000100000)
        ;; acct2 has 50000, order total would be 150000 > balance
        ;; But sell orders shouldn't check balance
        result (ord/validate-order sell-order acct2 inst1)]
    (is (nil? result)
        "validate-order should not check balance for sell orders")))

(deftest test-pending-orders
  ;; pending-orders should only return orders with pending status
  (let [cancelled (ord/cancel-order order2 "user request")
        orders [order1 cancelled order3]
        pending (ord/pending-orders orders)]
    (is (= 2 (count pending))
        "pending-orders should exclude non-pending orders")
    (is (every? #(= "pending" (:status %)) pending)
        "all returned orders should have pending status")))

;; ============================================================
;; trades module
;; ============================================================

(deftest test-execute-trade-id
  ;; execute-trade should use the provided trade-id, not the order-id
  (let [t (trd/execute-trade 2001 order1 15000 1000000400000)]
    (is (= 2001 (:id t))
        "execute-trade should use the provided trade-id")
    (is (= 1001 (:order-id t))
        "execute-trade should store the order's id as order-id")))

(deftest test-trade-pnl
  ;; trade-pnl should compute (current-price - exec-price) * quantity for buys
  ;; trade1: buy 10 @ 15000, current = 16000 -> pnl = (16000-15000)*10 = 10000
  (let [pnl (trd/trade-pnl trade1 16000)]
    (is (= 10000 pnl)
        "trade-pnl should multiply price difference by quantity"))
  ;; No change: pnl = 0
  (let [pnl (trd/trade-pnl trade1 15000)]
    (is (= 0 pnl)
        "trade-pnl should be zero when current equals exec price"))
  ;; Loss: current < exec for buy
  (let [pnl (trd/trade-pnl trade1 14000)]
    (is (= -10000 pnl)
        "trade-pnl should be negative for buy when current is below exec")))

(deftest test-settle-trade
  ;; settle-trade should adjust the account balance by the trade's net amount
  ;; trade1 is a buy: net = -(total+fee). total=150000, fee=150000*10/10000=150
  ;; settle should debit acct1 by (total+fee) = 150150
  (let [settled (trd/settle-trade trade1 acct1)]
    (is (= (- 100000 150150) (:balance settled))
        "settle-trade should adjust account balance by net amount"))
  ;; trade2 is a sell: net = total-fee = 1400000-1400 = 1398600 (credit)
  (let [settled (trd/settle-trade trade2 acct2)]
    (is (= (+ 50000 1398600) (:balance settled))
        "settle-trade for sell should credit account")))

(deftest test-trade-net-amount
  ;; trade-net-amount should return the net proceeds/cost of a trade
  ;; trade1: buy, total=150000, fee=150 -> net = -(150000+150) = -150150
  (let [net (trd/trade-net-amount trade1)]
    (is (= -150150 net)
        "trade-net-amount for buy should be negative (cost)"))
  ;; trade2: sell, total=1400000, fee=1400 -> net = 1400000-1400 = 1398600
  (let [net (trd/trade-net-amount trade2)]
    (is (= 1398600 net)
        "trade-net-amount for sell should be positive (proceeds)")))

(deftest test-execute-trade-timestamp
  ;; execute-trade should accept and store all four arguments including timestamp
  (let [t (trd/execute-trade 2003 order1 14800 1000000600000)]
    (is (= 1000000600000 (:executed-at t))
        "execute-trade should store the timestamp")
    (is (= 14800 (:exec-price t))
        "execute-trade should store exec-price")
    (is (= 10 (:quantity t))
        "execute-trade should pull quantity from order")))

(deftest test-trades-for-account
  ;; trades-for-account should filter trades by account-id
  (let [trades [trade1 trade2]
        alice-trades (trd/trades-for-account trades 1)]
    (is (= 1 (count alice-trades))
        "trades-for-account should filter by account-id"))
  (let [trades [trade1 trade2]
        bob-trades (trd/trades-for-account trades 2)]
    (is (= 1 (count bob-trades))
        "trades-for-account should find the correct account's trades")))

(deftest test-avg-exec-price
  ;; avg-exec-price should compute quantity-weighted average
  ;; Two buy trades: trade1 (10 @ 15000), another (5 @ 14800)
  ;; avg = (15000*10 + 14800*5) / (10+5) = (150000+74000)/15 = 14933
  (let [trade3 (trd/execute-trade 2003 order3 14800 1000000600000)
        avg (trd/avg-exec-price [trade1 trade3])]
    (is (= 14933 avg)
        "avg-exec-price should weight by quantity")))

(deftest test-trade-fee
  ;; trade-fee should be 10 basis points of the trade total
  ;; trade1: total = 15000*10 = 150000, fee = 150000*10/10000 = 150
  (let [fee (:fee trade1)]
    (is (= 150 fee)
        "trade fee should be 10 bps of the total"))
  ;; trade2: total = 280000*5 = 1400000, fee = 1400000*10/10000 = 1400
  (let [fee (:fee trade2)]
    (is (= 1400 fee)
        "trade fee should scale proportionally with total")))

(deftest test-trade-pnl-sell
  ;; trade-pnl for sell should compute (exec-price - current-price) * quantity
  ;; trade2: sell 5 @ 280000, current = 270000 -> pnl = (280000-270000)*5 = 50000
  (let [pnl (trd/trade-pnl trade2 270000)]
    (is (= 50000 pnl)
        "trade-pnl for sell should profit when current is below exec"))
  ;; sell at loss: current > exec -> negative pnl
  (let [pnl (trd/trade-pnl trade2 290000)]
    (is (= -50000 pnl)
        "trade-pnl for sell should be negative when current exceeds exec")))

;; ============================================================
;; ledger module
;; ============================================================

(deftest test-record-trade
  ;; record-trade should create a ledger entry with the trade as reference
  (let [entry (ldg/record-trade 5001 acct1 trade1 1000000700000)]
    (is (= (:id trade1) (:ref-id entry))
        "record-trade should use trade-id as ref-id")
    (is (= "trade" (:entry-type entry))
        "record-trade entry-type should be 'trade'")
    (is (= 1000000700000 (:timestamp entry))
        "record-trade should store the provided timestamp")
    (is (= 1 (:account-id entry))
        "record-trade should use the account's id")))

(deftest test-entries-for-account
  ;; entries-for-account should filter entries by account-id
  (let [e1 (ldg/record-trade 5001 acct1 trade1 1000000700000)
        e2 (ldg/record-trade 5002 acct2 trade2 1000000800000)
        entries [e1 e2]
        alice-entries (ldg/entries-for-account entries 1)]
    (is (= 1 (count alice-entries))
        "entries-for-account should match on account-id"))
  (let [e1 (ldg/record-trade 5001 acct1 trade1 1000000700000)
        e2 (ldg/record-trade 5002 acct2 trade2 1000000800000)
        entries [e1 e2]
        bob-entries (ldg/entries-for-account entries 2)]
    (is (= 1 (count bob-entries))
        "entries-for-account should find the correct account's entries")))

(deftest test-net-flow
  ;; net-flow should sum the amount field of all entries
  (let [e1 (ldg/record-deposit 5003 acct1 20000 1000000900000)
        e2 (ldg/record-withdrawal 5004 acct1 5000 1000001000000)
        entries [e1 e2]
        flow (ldg/net-flow entries)]
    ;; deposit = +20000, withdrawal amount stored as -5000 -> net = 15000
    (is (= 15000 flow)
        "net-flow should sum the amount fields of all entries"))
  ;; Single deposit
  (let [e1 (ldg/record-deposit 5005 acct1 30000 1000000900000)
        flow (ldg/net-flow [e1])]
    (is (= 30000 flow)
        "net-flow of single deposit should equal the deposit amount")))

(deftest test-record-deposit
  ;; record-deposit should return a properly constructed ledger entry
  (let [entry (ldg/record-deposit 5003 acct1 20000 1000000900000)]
    (is (= 5003 (:id entry))
        "record-deposit should return a ledger entry with the given id")
    (is (= 1 (:account-id entry))
        "record-deposit entry should have the correct account-id")
    (is (= "deposit" (:entry-type entry))
        "record-deposit entry-type should be 'deposit'")
    (is (= 120000 (:balance-after entry))
        "record-deposit balance-after should reflect the new balance")))

(deftest test-record-withdrawal
  ;; record-withdrawal should create a ledger entry for the withdrawal
  (let [entry (ldg/record-withdrawal 5004 acct1 5000 1000001000000)]
    (is (= 1 (:account-id entry))
        "record-withdrawal should use the account's id")
    (is (= "withdrawal" (:entry-type entry))
        "record-withdrawal entry-type should be 'withdrawal'")
    (is (= 95000 (:balance-after entry))
        "record-withdrawal balance-after should reflect the reduced balance"))
  (let [entry (ldg/record-withdrawal 5005 acct2 10000 1000001000000)]
    (is (= 2 (:account-id entry))
        "record-withdrawal should use the correct account's id")))

(deftest test-reconcile
  ;; reconcile should validate that entries are consistent with the account balance
  (let [deposit-entry (ldg/record-deposit 5005 acct1 100000 1000000000000)
        entries [deposit-entry]]
    (is (true? (ldg/reconcile entries acct1))
        "reconcile should return true when entries are consistent"))
  ;; With no entries, net-flow=0, initial = balance - 0 = 100000 >= 0 -> true
  (is (true? (ldg/reconcile [] acct1))
      "reconcile with empty entries should return true"))

;; ============================================================
;; reports module
;; ============================================================

(deftest test-calculate-position
  ;; calculate-position should compute net quantity and current value from trades
  ;; trade1: buy 10 AAPL, no sells -> net_qty=10, value=15000*10=150000
  (let [pos (rpt/calculate-position [trade1] 101 15000 "AAPL")]
    (is (= 10 (:quantity pos))
        "calculate-position should compute net quantity from trades")
    (is (= 150000 (:current-value pos))
        "calculate-position current-value should be price times net quantity")
    (is (= "AAPL" (:ticker pos))
        "calculate-position should carry the ticker")
    (is (= 101 (:instrument-id pos))
        "calculate-position should carry instrument-id")))

(deftest test-portfolio-value
  ;; portfolio-value should sum the current-value of all positions
  (let [pos1 (rpt/calculate-position [trade1] 101 16000 "AAPL")
        ;; pos1: net_qty = 10-0 = 10, value = 16000*10 = 160000
        total-single (rpt/portfolio-value [pos1])]
    (is (= 160000 total-single)
        "portfolio-value of single position should equal its current-value"))
  (let [pos1 (rpt/calculate-position [trade1] 101 16000 "AAPL")
        pos2 (rpt/calculate-position [trade2] 102 275000 "GOOG")
        ;; pos2: net_qty = 0-5 = -5, value = 275000*(-5) = -1375000
        total (rpt/portfolio-value [pos1 pos2])]
    (is (= (+ 160000 -1375000) total)
        "portfolio-value should sum current-value across all positions")))

(deftest test-total-pnl
  ;; total-pnl should return the sum of pnl across all trades
  ;; trade1: buy 10@15000, current=16000, pnl=(16000-15000)*10=10000
  ;; trade2: sell 5@280000, current=270000, pnl=(280000-270000)*5=50000
  (let [pnl (rpt/total-pnl [trade1 trade2] [16000 270000] [101 102])]
    (is (= 60000 pnl)
        "total-pnl should return the sum of all trade pnl amounts"))
  ;; Single instrument
  (let [pnl (rpt/total-pnl [trade1] [16000] [101])]
    (is (= 10000 pnl)
        "total-pnl for single trade should equal that trade's pnl")))

(deftest test-format-pnl-report
  ;; format-pnl-report should produce a human-readable string
  (let [report (rpt/generate-pnl-report 1 [trade1] [16000] [101] 1000001000000)
        formatted (rpt/format-pnl-report report)]
    (is (string? formatted)
        "format-pnl-report should return a string")
    (is (.contains formatted "1 trades")
        "format-pnl-report should include trade count")))

(deftest test-account-summary
  ;; account-summary should produce a formatted summary string
  (let [entries [(ldg/record-trade 5001 acct1 trade1 1000000700000)]
        summary (rpt/account-summary acct1 [trade1] entries)]
    (is (string? summary)
        "account-summary should return a string")
    (is (.contains summary "Alice")
        "account-summary should include account name")
    (is (.contains summary "1 trades")
        "account-summary should include trade count")
    (is (.contains summary "1 ledger entries")
        "account-summary should include entry count")
    (is (.contains summary "USD")
        "account-summary should include formatted balance")))

(deftest test-generate-pnl-report
  ;; generate-pnl-report should build a report record with correct fields
  (let [report (rpt/generate-pnl-report 1 [trade1] [16000] [101] 1000001000000)]
    (is (= 1000001000000 (:as-of report))
        "generate-pnl-report as-of should be the provided timestamp")
    (is (= 1 (:account-id report))
        "generate-pnl-report should set correct account-id")
    (is (= 1 (:num-trades report))
        "generate-pnl-report should count trades")
    (is (integer? (:as-of report))
        "generate-pnl-report as-of should be an integer timestamp")))

(deftest test-calculate-position-sells
  ;; calculate-position should subtract sell quantities from buy quantities
  ;; Buy 10 then sell 3 -> net = 7
  (let [sell-order (ord/create-order 1005 1 101 "sell" 3 15500 1000000700000)
        sell-trade (trd/execute-trade 2004 sell-order 15500 1000000800000)
        pos (rpt/calculate-position [trade1 sell-trade] 101 16000 "AAPL")]
    (is (= 7 (:quantity pos))
        "calculate-position should subtract sell quantity from buy quantity")
    (is (= (* 16000 7) (:current-value pos))
        "current-value should use net quantity"))
  ;; Sell all: net = 0
  (let [sell-order (ord/create-order 1006 1 101 "sell" 10 15500 1000000700000)
        sell-trade (trd/execute-trade 2005 sell-order 15500 1000000800000)
        pos (rpt/calculate-position [trade1 sell-trade] 101 16000 "AAPL")]
    (is (= 0 (:quantity pos))
        "selling all should result in zero net quantity")))

;; ============================================================
;; Run tests and report score
;; ============================================================

(defn -main []
  (let [results (run-tests 'verify.behavioral-stripped)
        pass (:pass results)
        fail (:fail results)
        error (:error results)
        total-fail (+ fail error)
        total (+ pass total-fail)]
    (println)
    (println (str pass "/" total " assertions passed"))))

(-main)
