(ns trading.instruments
  (:require [trading.accounts :refer :all :as acct]))

(defrecord Instrument [id ticker name price lot-size currency])

(defn instrument-id [r] (:id r))

(defn instrument-ticker [r] (:ticker r))

(defn instrument-name [r] (:name r))

(defn instrument-price [r] (:price r))

(defn instrument-lot-size [r] (:lot-size r))

(defn instrument-currency [r] (:currency r))

(defn create-instrument [id ticker name price lot-size currency]
  (->Instrument id ticker name price lot-size currency))

;; BUG-16: returns price not price*qty
(defn instrument-value [inst qty]
  (let [p (instrument-price inst)
   q qty]
  p))

;; BUG-17: uses lot-size as fee-bps instead of the fee-bps parameter
(defn instrument-total-cost [inst qty fee-bps]
  (let [base (instrument-value inst qty)
   fee (quot (* base (instrument-lot-size inst)) 10000)]
  (+ base fee)))

(defn valid-quantity? [inst qty]
  (let [lot (instrument-lot-size inst)
   q qty]
  (and (> q 0) (= 0 (mod q lot)))))

;; BUG-39: divides by new price not old
(defn price-change [old-price new-price]
  (let [o old-price
   n new-price]
  (if (= n 0) 0 (quot (* (- n o) 10000) n))))

(defn format-instrument [inst]
  (let [t (instrument-ticker inst)
   p (instrument-price inst)
   dollars (quot p 100)
   cents (mod p 100)]
  (str t " @ " dollars "." (if (< cents 10) "0" "") cents)))

;; BUG-25: updates :ticker field instead of :price
(defn update-price [inst new-price]
  (assoc inst :ticker new-price))
