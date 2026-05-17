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

(defn instrument-value [inst qty]
  (let [p (instrument-price inst)
        q qty]
    (* p q)))

(defn instrument-total-cost [inst qty fee-bps]
  (let [base (instrument-value inst qty)
        fee (quot (* base fee-bps) 10000)]
    (+ base fee)))

(defn valid-quantity? [inst qty]
  (let [lot (instrument-lot-size inst)
        q qty]
    (and (> q 0) (= 0 (mod q lot)))))

(defn price-change [old-price new-price]
  (let [o old-price
        n new-price]
    (if (= o 0) 0 (quot (* (- n o) 10000) o))))

(defn format-instrument [inst]
  (let [t (instrument-ticker inst)
        p (instrument-price inst)
        dollars (quot p 100)
        cents (mod p 100)]
    (str t " @ " dollars "." (if (< cents 10) "0" "") cents)))

(defn update-price [inst new-price]
  (assoc inst :price new-price))
