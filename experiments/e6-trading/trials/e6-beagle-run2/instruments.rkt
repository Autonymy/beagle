#lang beagle

(ns trading.instruments)

(require trading.accounts :as acct)

;; --- scalar types ---

(defscalar InstrumentId Long)
(defscalar Ticker String)
(defscalar Price Long)
(defscalar Quantity Long)

;; --- records ---

(defrecord Instrument
  [(id : InstrumentId)
   (ticker : Ticker)
   (name : String)
   (price : Price)
   (lot-size : Quantity)
   (currency : Currency)])

;; --- constructors ---

(defn create-instrument
  [(id : Long) (ticker : String) (name : String)
   (price : Long) (lot-size : Long) (currency : String)] : Instrument
  (->Instrument (->InstrumentId id) (->Ticker ticker) name
                (->Price price) (->Quantity lot-size)
                (->Currency currency)))

;; --- operations ---

;; BUG-16: returns price not price*qty
(defn instrument-value [(inst : Instrument) (qty : Quantity)] : Amount
  (let [p (price-value (instrument-price inst))
        q (quantity-value qty)]
    (->Amount (* p q))))

;; BUG-17: uses lot-size as fee-bps
(defn instrument-total-cost [(inst : Instrument) (qty : Quantity) (fee-bps : Long)] : Amount
  (let [base (amount-value (instrument-value inst qty))
        fee (quot (* base fee-bps) 10000)]
    (->Amount (+ base fee))))

(defn valid-quantity? [(inst : Instrument) (qty : Quantity)] : Boolean
  (let [lot (quantity-value (instrument-lot-size inst))
        q (quantity-value qty)]
    (and (> q 0) (= 0 (mod q lot)))))

;; BUG-39: divides by new price not old
(defn price-change [(old-price : Price) (new-price : Price)] : Long
  (let [o (price-value old-price)
        n (price-value new-price)]
    (if (= o 0) 0 (quot (* (- n o) 10000) o))))

(defn format-instrument [(inst : Instrument)] : String
  (let [t (ticker-value (instrument-ticker inst))
        p (price-value (instrument-price inst))
        dollars (quot p 100)
        cents (mod p 100)]
    (str t " @ " dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-25: wrong field name in with
(defn update-price [(inst : Instrument) (new-price : Price)] : Instrument
  (with inst [:price new-price]))
