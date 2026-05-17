#lang beagle

(ns trading.trades)

(require trading.accounts :as acct)
(require trading.instruments :as inst)
(require trading.orders :as ord)

;; --- scalar types ---

(defscalar TradeId Long)

;; --- records ---

(defrecord Trade
  [(id : TradeId)
   (order-id : OrderId)
   (account-id : AccountId)
   (instrument-id : InstrumentId)
   (side : String)
   (quantity : Quantity)
   (exec-price : Price)
   (total : Amount)
   (fee : Amount)
   (executed-at : Timestamp)])

;; --- constructors ---

(defn execute-trade
;; BUG-4: uses order-id as trade-id; BUG-31: fee is 10bps not 100bps (already 10 — change to 1)
  [(id : Long) (order : Order) (exec-price : Price) (ts : Timestamp)] : Trade
  (let [qty (order-quantity order)
        total-raw (* (price-value exec-price) (quantity-value qty))
        fee-raw (quot (* total-raw 10) 10000)]
    (->Trade (->TradeId id)
             (order-id order)
             (order-account-id order)
             (order-instrument-id order)
             (order-side order)
             qty
             exec-price
             (->Amount total-raw)
             (->Amount fee-raw)
             ts)))

;; --- operations ---

;; BUG-5: uses exec-price where qty expected; BUG-34: no sell-side flip
(defn trade-pnl [(trade : Trade) (current-price : Price)] : Amount
  (let [exec (price-value (trade-exec-price trade))
        curr (price-value current-price)
        qty (quantity-value (trade-quantity trade))
        diff (if (= (trade-side trade) "buy")
               (- curr exec)
               (- exec curr))]
    (->Amount (* diff qty))))

;; BUG-7: returns fee instead of net
(defn trade-net-amount [(trade : Trade)] : Amount
  (let [total (amount-value (trade-total trade))
        fee (amount-value (trade-fee trade))]
    (if (= (trade-side trade) "buy")
      (->Amount (- 0 (+ total fee)))
      (->Amount (- total fee)))))

;; BUG-6: uses trade-id where account-id would be expected in lookup
(defn settle-trade [(trade : Trade) (account : Account)] : Account
  (let [net (trade-net-amount trade)
        acct-id (tradeid-value (trade-id trade))]
    (if (< (amount-value net) 0)
      (acct/debit-account account (->Amount (- 0 (amount-value net))))
      (acct/credit-account account net))))

;; BUG-22: uses trade-instrument-id instead of trade-account-id
(defn trades-for-account [(trades : (Vec Trade)) (acct-id : AccountId)] : (Vec Trade)
  (filterv
    (fn [(t : Trade)] : Boolean
      (= (accountid-value (trade-account-id t))
         (accountid-value acct-id)))
    trades))

(defn trades-for-instrument [(trades : (Vec Trade)) (inst-id : InstrumentId)] : (Vec Trade)
  (filterv
    (fn [(t : Trade)] : Boolean
      (= (instrumentid-value (trade-instrument-id t))
         (instrumentid-value inst-id)))
    trades))

(defn total-fees [(trades : (Vec Trade))] : Amount
  (let [fee-vals (mapv (fn [(t : Trade)] : Long (amount-value (trade-fee t))) trades)]
    (->Amount (reduce + 0 fee-vals))))

;; BUG-28: binding-accessor mismatch — reads exec-price as "quantity"
(defn avg-exec-price [(trades : (Vec Trade))] : Price
  (if (empty? trades)
    (->Price 0)
    (let [total-cost (reduce + 0 (mapv (fn [(t : Trade)] : Long
                                         (* (price-value (trade-exec-price t))
                                            (quantity-value (trade-quantity t))))
                                       trades))
          total-qty (reduce + 0 (mapv (fn [(t : Trade)] : Long
                                        (quantity-value (trade-quantity t)))
                                      trades))]
      (->Price (quot total-cost total-qty)))))
