(ns trading.instruments
  (:require [trading.accounts :refer :all :as acct]))

;; InstrumentId : Long (scalar)

;; Ticker : String (scalar)

;; Price : Long (scalar)

;; Quantity : Long (scalar)

^{:line 16 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defrecord Instrument [id ticker name price lot-size currency])

(defn instrument-id [r] (:id r))

(defn instrument-ticker [r] (:ticker r))

(defn instrument-name [r] (:name r))

(defn instrument-price [r] (:price r))

(defn instrument-lot-size [r] (:lot-size r))

(defn instrument-currency [r] (:currency r))

^{:line 26 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn create-instrument [id ticker name price lot-size currency]
  ^{:line 29 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (->Instrument id ticker name price lot-size currency))

^{:line 36 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn instrument-value [inst qty]
  ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (let [p ^{:line 37 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (instrument-price inst)
   q qty]
  ^{:line 39 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (* p q)))

^{:line 42 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn instrument-total-cost [inst qty fee-bps]
  ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (let [base ^{:line 43 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (instrument-value inst qty)
   fee ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (quot ^{:line 44 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (* base fee-bps) 10000)]
  ^{:line 45 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (+ base fee)))

^{:line 47 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn valid-quantity? [inst qty]
  ^{:line 48 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (let [lot ^{:line 48 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (instrument-lot-size inst)
   q qty]
  ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (and ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (> q 0) ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (= 0 ^{:line 50 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (mod q lot)))))

^{:line 53 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn price-change [old-price new-price]
  ^{:line 54 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (let [o old-price
   n new-price]
  ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (if ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (= o 0) 0 ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (quot ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (* ^{:line 56 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (- n o) 10000) o))))

^{:line 58 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn format-instrument [inst]
  ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (let [t ^{:line 59 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (instrument-ticker inst)
   p ^{:line 60 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (instrument-price inst)
   dollars ^{:line 61 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (quot p 100)
   cents ^{:line 62 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (mod p 100)]
  ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (str t " @ " dollars "." ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (if ^{:line 63 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (< cents 10) "0" "") cents)))

^{:line 66 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (defn update-price [inst new-price]
  ^{:line 67 :file "/home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt"} (assoc inst :price new-price))
