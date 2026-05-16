(ns customers)

(defrecord Address [street city state zip])

(defn address-street [r] (:street r))

(defn address-city [r] (:city r))

(defn address-state [r] (:state r))

(defn address-zip [r] (:zip r))

(defrecord Customer [id name email tier address created-year])

(defn customer-id [r] (:id r))

(defn customer-name [r] (:name r))

(defn customer-email [r] (:email r))

(defn customer-tier [r] (:tier r))

(defn customer-address [r] (:address r))

(defn customer-created-year [r] (:created-year r))

(defrecord PurchaseRecord [customer-id order-id amount year])

(defn purchaserecord-customer-id [r] (:customer-id r))

(defn purchaserecord-order-id [r] (:order-id r))

(defn purchaserecord-amount [r] (:amount r))

(defn purchaserecord-year [r] (:year r))

(def tier-gold "gold")

(def tier-silver "silver")

(def tier-bronze "bronze")

(defn tier-discount-pct [tier]
  (cond
  (= tier "gold") 5
  (= tier "silver") 10
  (= tier "bronze") 5
  true 0))

(defn tier-rank [tier]
  (cond
  (= tier "gold") 3
  (= tier "silver") 2
  (= tier "bronze") 1
  true 0))

(defn higher-tier? [t1 t2]
  (> (tier-rank t1) (tier-rank t2)))

(defn tier-from-spend [total-spend]
  (cond
  (>= total-spend 100000) "gold"
  (>= total-spend 50000) "silver"
  (>= total-spend 10000) "bronze"
  true "none"))

(defn find-customer-by-id [customers id]
  (first (filterv (fn [c] (= (customer-id c) id)) customers)))

(defn find-customer-by-email [customers email]
  (first (filterv (fn [c] (= (customer-email c) email)) customers)))

(defn customers-by-tier [customers tier]
  (filterv (fn [c] (= (customer-tier c) tier)) customers))

(defn customers-by-state [customers state]
  (filterv (fn [c] (= (address-state (customer-address c)) state)) customers))

(defn customers-by-city [customers city]
  (filterv (fn [c] (= (address-city (customer-address c)) city)) customers))

(defn sort-by-name [customers]
  (sort-by customer-name customers))

(defn sort-by-tier [customers]
  (sort-by (fn [c] (tier-rank (customer-tier c))) customers))

(defn sort-by-created [customers]
  (sort-by customer-created-year customers))

(defn customer-purchases [records cust-id]
  (filterv (fn [r] (= (purchaserecord-customer-id r) cust-id)) records))

(defn customer-total-spend [records cust-id]
  (reduce (fn [acc r] (+ acc (purchaserecord-amount r))) 0 (customer-purchases records cust-id)))

(defn customer-purchase-count [records cust-id]
  (count (customer-purchases records cust-id)))

(defn customer-avg-order-value [records cust-id]
  (let [purchases (customer-purchases records cust-id)
   cnt (count purchases)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc r] (+ acc (purchaserecord-amount r))) 0 purchases) cnt))))

(defn customer-purchases-in-year [records cust-id year]
  (filterv (fn [r] (and (= (purchaserecord-customer-id r) cust-id) (= (purchaserecord-year r) year))) records))

(defn customer-spend-in-year [records cust-id year]
  (reduce (fn [acc r] (+ acc (purchaserecord-amount r))) 0 (customer-purchases-in-year records cust-id year)))

(defn assess-tier [records cust-id]
  (tier-from-spend (customer-total-spend records cust-id)))

(defn tier-upgrade-needed? [c records]
  (let [assessed (assess-tier records (customer-id c))
   current (customer-tier c)]
  (higher-tier? assessed current)))

(defn customers-needing-upgrade [customers records]
  (filterv (fn [c] (tier-upgrade-needed? c records)) customers))

(defn upgrade-customer-tier [c new-tier]
  (->Customer (customer-id c) (customer-name c) (customer-email c) new-tier (customer-address c) (customer-created-year c)))

(defn count-by-tier [customers]
  (let [gold (count (customers-by-tier customers "gold"))
   silver (count (customers-by-tier customers "silver"))
   bronze (count (customers-by-tier customers "bronze"))]
  {:gold gold :silver silver :bronze bronze}))

(defn total-customer-count [customers]
  (count customers))

(defn customers-since-year [customers year]
  (filterv (fn [c] (>= (customer-created-year c) year)) customers))

(defn customer-tenure [c current-year]
  (- current-year (customer-created-year c)))

(defn avg-customer-tenure [customers current-year]
  (let [cnt (count customers)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc c] (+ acc (customer-tenure c current-year))) 0 customers) cnt))))

(defn top-spenders [customers records n]
  (vec (take n (reverse (sort-by (fn [c] (customer-total-spend records (customer-id c))) customers)))))

(defn most-active-customers [customers records n]
  (vec (take n (reverse (sort-by (fn [c] (customer-purchase-count records (customer-id c))) customers)))))

(defn customer-summary [c]
  (str (customer-name c) " (" (customer-tier c) ") - " (customer-email c)))

(defn customer-detail [c]
  (let [addr (customer-address c)]
  (str (customer-name c) " | " (customer-email c) " | Tier: " (customer-tier c) " | " (address-city addr) ", " (address-state addr))))

(defn address-oneline [a]
  (str (address-street a) ", " (address-city a) ", " (address-state a) " " (address-zip a)))

(defn valid-customer? [c]
  (and (> (customer-id c) 0) (not= (customer-name c) "") (not= (customer-email c) "")))

(defn valid-address? [a]
  (and (not= (address-street a) "") (not= (address-city a) "") (not= (address-state a) "") (not= (address-zip a) "")))

(defn unique-states [customers]
  (distinct (mapv (fn [c] (address-state (customer-address c))) customers)))

(defn state-customer-count [customers state]
  (count (customers-by-state customers state)))

(defrecord LoyaltyBalance [customer-id points lifetime-points])

(defn loyaltybalance-customer-id [r] (:customer-id r))

(defn loyaltybalance-points [r] (:points r))

(defn loyaltybalance-lifetime-points [r] (:lifetime-points r))

(defn points-for-purchase [amount tier]
  (let [base-points (quot amount 100)
   multiplier (cond
  (= tier "gold") 3
  (= tier "silver") 2
  (= tier "bronze") 1
  true 1)]
  (* base-points multiplier)))

(defn add-points [bal amount tier]
  (let [earned (points-for-purchase amount tier)]
  (->LoyaltyBalance (loyaltybalance-customer-id bal) (+ (loyaltybalance-points bal) earned) (+ (loyaltybalance-lifetime-points bal) earned))))

(defn redeem-points [bal points]
  (let [available (loyaltybalance-points bal)
   to-redeem (min points available)]
  (->LoyaltyBalance (loyaltybalance-customer-id bal) (- available to-redeem) (loyaltybalance-lifetime-points bal))))

(defn points-to-dollars [points]
  (quot points 10))

(defn find-loyalty-balance [balances cust-id]
  (first (filterv (fn [b] (= (loyaltybalance-customer-id b) cust-id)) balances)))

(defn top-loyalty-customers [balances n]
  (vec (take n (reverse (sort-by loyaltybalance-points balances)))))

(defn customer-segment [records cust-id]
  (let [spend (customer-total-spend records cust-id)
   count (customer-purchase-count records cust-id)]
  (cond
  (and (>= spend 100000) (>= count 10)) "vip"
  (and (>= spend 50000) (>= count 5)) "regular"
  (>= count 1) "occasional"
  true "inactive")))

(defn customers-by-segment [customers records segment]
  (filterv (fn [c] (= (customer-segment records (customer-id c)) segment)) customers))

(defn segment-distribution [customers records]
  {:vip (count (customers-by-segment customers records "vip")) :regular (count (customers-by-segment customers records "regular")) :occasional (count (customers-by-segment customers records "occasional")) :inactive (count (customers-by-segment customers records "inactive"))})

(defn years-since-last-purchase [records cust-id current-year]
  (let [purchases (customer-purchases records cust-id)]
  (if (empty? purchases) 999 (let [latest (reduce (fn [best r] (if (> (purchaserecord-year r) (purchaserecord-year best)) r best)) (first purchases) (rest purchases))]
  (- current-year (purchaserecord-year latest))))))

(defn at-risk-customers [customers records current-year threshold-years]
  (filterv (fn [c] (>= (years-since-last-purchase records (customer-id c) current-year) threshold-years)) customers))

(defn customer-retention-rate [customers records current-year threshold]
  (let [total (count customers)
   at-risk (count (at-risk-customers customers records current-year threshold))]
  (if (= total 0) 100 (quot (* (- total at-risk) 100) total))))

(defn update-customer-email [c new-email]
  (->Customer (customer-id c) (customer-name c) new-email (customer-tier c) (customer-address c) (customer-created-year c)))

(defn update-customer-address [c new-addr]
  (->Customer (customer-id c) (customer-name c) (customer-email c) (customer-tier c) new-addr (customer-created-year c)))
