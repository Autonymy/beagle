(ns inventory
  (:require [catalog :as cat]))

(defrecord Warehouse [id name location capacity])

(defn warehouse-id [r] (:id r))

(defn warehouse-name [r] (:name r))

(defn warehouse-location [r] (:location r))

(defn warehouse-capacity [r] (:capacity r))

(defrecord StockLevel [warehouse-id product-id quantity min-quantity])

(defn stocklevel-warehouse-id [r] (:warehouse-id r))

(defn stocklevel-product-id [r] (:product-id r))

(defn stocklevel-quantity [r] (:quantity r))

(defn stocklevel-min-quantity [r] (:min-quantity r))

(defrecord StockMovement [id warehouse-id product-id quantity-change movement-type timestamp])

(defn stockmovement-id [r] (:id r))

(defn stockmovement-warehouse-id [r] (:warehouse-id r))

(defn stockmovement-product-id [r] (:product-id r))

(defn stockmovement-quantity-change [r] (:quantity-change r))

(defn stockmovement-movement-type [r] (:movement-type r))

(defn stockmovement-timestamp [r] (:timestamp r))

(defn find-warehouse-by-id [warehouses id]
  (first (filterv (fn [w] (= (warehouse-id w) id)) warehouses)))

(defn warehouses-by-location [warehouses loc]
  (filterv (fn [w] (= (warehouse-location w) loc)) warehouses))

(defn sort-by-capacity [warehouses]
  (sort-by warehouse-capacity warehouses))

(defn find-stock [levels wh-id prod-id]
  (first (filterv (fn [s] (and (= (stocklevel-warehouse-id s) wh-id) (= (stocklevel-product-id s) prod-id))) levels)))

(defn stock-for-product [levels prod-id]
  (filterv (fn [s] (= (stocklevel-product-id s) prod-id)) levels))

(defn stock-for-warehouse [levels wh-id]
  (filterv (fn [s] (= (stocklevel-warehouse-id s) wh-id)) levels))

(defn total-stock-for-product [levels prod-id]
  (reduce (fn [acc s] (+ acc (stocklevel-quantity s))) 0 (stock-for-product levels prod-id)))

(defn total-stock-in-warehouse [levels wh-id]
  (reduce (fn [acc s] (+ acc (stocklevel-quantity s))) 0 (stock-for-warehouse levels wh-id)))

(defn in-stock? [levels wh-id prod-id]
  (let [sl (find-stock levels wh-id prod-id)]
  (if (nil? sl) false (> (stocklevel-quantity sl) 0))))

(defn below-minimum? [sl]
  (< (stocklevel-quantity sl) (stocklevel-min-quantity sl)))

(defn low-stock-items [levels]
  (filterv below-minimum? levels))

(defn out-of-stock-items [levels]
  (filterv (fn [s] (= (stocklevel-quantity s) 0)) levels))

(defn available-quantity [levels prod-id]
  (total-stock-for-product levels prod-id))

(defn can-fulfill? [levels prod-id qty]
  (>= (available-quantity levels prod-id) qty))

(defn stock-value-at-warehouse [levels wh-id products]
  (let [wh-stock (stock-for-warehouse levels wh-id)]
  (reduce (fn [acc sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))]
  (+ acc (* (stocklevel-quantity sl) cost)))) 0 wh-stock)))

(defn stock-value-for-product [levels prod-id products]
  (let [prod (cat/find-product-by-id products prod-id)
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))
   qty (total-stock-for-product levels prod-id)]
  (* qty cost)))

(defn total-inventory-value [levels products]
  (reduce (fn [acc sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))]
  (+ acc (* (stocklevel-quantity sl) cost)))) 0 levels))

(defn stock-retail-value [levels products]
  (reduce (fn [acc sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   price (if (nil? prod) 0 (cat/product-unit-price prod))]
  (+ acc (* (stocklevel-quantity sl) price)))) 0 levels))

(defn reorder-needed [levels]
  (filterv (fn [sl] (< (stocklevel-quantity sl) (stocklevel-min-quantity sl))) levels))

(defn reorder-quantity [sl]
  (let [deficit (- (stocklevel-min-quantity sl) (stocklevel-quantity sl))]
  (if (> deficit 0) (* deficit 2) 0)))

(defn reorder-list [levels products]
  (let [needed (reorder-needed levels)]
  (mapv (fn [sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   pname (if (nil? prod) "unknown" (cat/product-name prod))
   qty (reorder-quantity sl)]
  {:product-id (stocklevel-product-id sl) :product-name pname :warehouse-id (stocklevel-warehouse-id sl) :reorder-qty qty})) needed)))

(defn reorder-cost [levels products]
  (let [needed (reorder-needed levels)]
  (reduce (fn [acc sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))
   qty (reorder-quantity sl)]
  (+ acc (* qty cost)))) 0 needed)))

(defn movements-for-product [movements prod-id]
  (filterv (fn [m] (= (stockmovement-product-id m) prod-id)) movements))

(defn movements-for-warehouse [movements wh-id]
  (filterv (fn [m] (= (stockmovement-warehouse-id m) wh-id)) movements))

(defn movements-by-type [movements mtype]
  (filterv (fn [m] (= (stockmovement-movement-type m) mtype)) movements))

(defn net-movement [movements prod-id]
  (reduce (fn [acc m] (+ acc (stockmovement-quantity-change m))) 0 (movements-for-product movements prod-id)))

(defn total-received [movements prod-id]
  (reduce (fn [acc m] (+ acc (stockmovement-quantity-change m))) 0 (filterv (fn [m] (and (= (stockmovement-product-id m) prod-id) (= (stockmovement-movement-type m) "receive"))) movements)))

(defn total-shipped [movements prod-id]
  (reduce (fn [acc m] (+ acc (- 0 (stockmovement-quantity-change m)))) 0 (filterv (fn [m] (and (= (stockmovement-product-id m) prod-id) (= (stockmovement-movement-type m) "ship"))) movements)))

(defn apply-movement [levels mv]
  (let [wh-id (stockmovement-warehouse-id mv)
   prod-id (stockmovement-product-id mv)
   change (stockmovement-quantity-change mv)]
  (mapv (fn [sl] (if (and (= (stocklevel-warehouse-id sl) wh-id) (= (stocklevel-product-id sl) prod-id)) (->StockLevel wh-id prod-id (+ (stocklevel-quantity sl) change) (stocklevel-min-quantity sl)) sl)) levels)))

(defn apply-movements [levels movements]
  (reduce apply-movement levels movements))

(defn warehouse-item-count [levels wh-id]
  (count (stock-for-warehouse levels wh-id)))

(defn warehouse-total-units [levels wh-id]
  (total-stock-in-warehouse levels wh-id))

(defn warehouse-utilization-pct [levels wh]
  (let [total (warehouse-total-units levels (warehouse-id wh))
   cap (warehouse-capacity wh)]
  (if (= cap 0) 0 (quot (* total 100) cap))))

(defn stock-summary [sl]
  (str "WH:" (stocklevel-warehouse-id sl) " Prod:" (stocklevel-product-id sl) " Qty:" (stocklevel-quantity sl) "/" (stocklevel-min-quantity sl)))

(defn warehouse-summary [wh]
  (str (warehouse-name wh) " (" (warehouse-location wh) ") cap:" (warehouse-capacity wh)))

(defn stock-with-product-names [levels products]
  (mapv (fn [sl] (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
   pname (if (nil? prod) "unknown" (cat/product-name prod))]
  {:product-name pname :warehouse-id (stocklevel-warehouse-id sl) :quantity (stocklevel-quantity sl) :min-quantity (stocklevel-min-quantity sl)})) levels))

(defn products-out-of-stock [levels products]
  (let [oos-ids (mapv (fn [sl] (stocklevel-product-id sl)) (out-of-stock-items levels))]
  (filterv (fn [p] (some (fn [id] (= (cat/product-id p) id)) oos-ids)) products)))

(defrecord Transfer [id from-warehouse to-warehouse product-id quantity timestamp])

(defn transfer-id [r] (:id r))

(defn transfer-from-warehouse [r] (:from-warehouse r))

(defn transfer-to-warehouse [r] (:to-warehouse r))

(defn transfer-product-id [r] (:product-id r))

(defn transfer-quantity [r] (:quantity r))

(defn transfer-timestamp [r] (:timestamp r))

(defn create-transfer [id from-wh to-wh prod-id qty ts]
  (->Transfer id from-wh to-wh prod-id qty ts))

(defn can-transfer? [levels from-wh prod-id qty]
  (let [sl (find-stock levels from-wh prod-id)]
  (if (nil? sl) false (>= (stocklevel-quantity sl) qty))))

(defn apply-transfer [levels tr]
  (let [from-wh (transfer-from-warehouse tr)
   to-wh (transfer-to-warehouse tr)
   prod-id (transfer-product-id tr)
   qty (transfer-quantity tr)]
  (mapv (fn [sl] (cond
  (and (= (stocklevel-warehouse-id sl) from-wh) (= (stocklevel-product-id sl) prod-id)) (->StockLevel from-wh prod-id (- (stocklevel-quantity sl) qty) (stocklevel-min-quantity sl))
  (and (= (stocklevel-warehouse-id sl) to-wh) (= (stocklevel-product-id sl) prod-id)) (->StockLevel to-wh prod-id (+ (stocklevel-quantity sl) qty) (stocklevel-min-quantity sl))
  true sl)) levels)))

(defn transfers-for-product [transfers prod-id]
  (filterv (fn [t] (= (transfer-product-id t) prod-id)) transfers))

(defn transfers-from-warehouse [transfers wh-id]
  (filterv (fn [t] (= (transfer-from-warehouse t) wh-id)) transfers))

(defn transfers-to-warehouse [transfers wh-id]
  (filterv (fn [t] (= (transfer-to-warehouse t) wh-id)) transfers))

(defn product-stock-value [levels prod-id products]
  (stock-value-for-product levels prod-id products))

(defn classify-abc [value total-value]
  (let [pct (if (= total-value 0) 0 (quot (* value 100) total-value))]
  (cond
  (>= pct 20) "A"
  (>= pct 5) "B"
  true "C")))

(defn abc-analysis [levels products]
  (let [total (total-inventory-value levels products)]
  (mapv (fn [p] (let [pid (cat/product-id p)
   value (product-stock-value levels pid products)
   class (classify-abc value total)]
  {:product-id pid :product-name (cat/product-name p) :stock-value value :abc-class class})) products)))

(defn safety-stock [avg-daily-demand lead-time-days safety-factor]
  (* avg-daily-demand lead-time-days safety-factor))

(defn recommended-min-quantity [avg-daily-demand supplier safety-factor]
  (let [lead-time (if (nil? supplier) 7 (cat/supplier-lead-time-days supplier))]
  (safety-stock avg-daily-demand lead-time safety-factor)))

(defn batch-receive [levels wh-id items]
  (reduce (fn [lvls item] (let [prod-id (first item)
   qty (second item)]
  (mapv (fn [sl] (if (and (= (stocklevel-warehouse-id sl) wh-id) (= (stocklevel-product-id sl) prod-id)) (->StockLevel wh-id prod-id (+ (stocklevel-quantity sl) qty) (stocklevel-min-quantity sl)) sl)) lvls))) levels items))

(defn batch-ship [levels wh-id items]
  (reduce (fn [lvls item] (let [prod-id (first item)
   qty (second item)]
  (mapv (fn [sl] (if (and (= (stocklevel-warehouse-id sl) wh-id) (= (stocklevel-product-id sl) prod-id)) (->StockLevel wh-id prod-id (- (stocklevel-quantity sl) qty) (stocklevel-min-quantity sl)) sl)) lvls))) levels items))

(defn days-of-stock [levels prod-id daily-demand]
  (let [qty (total-stock-for-product levels prod-id)]
  (if (= daily-demand 0) 999 (quot qty daily-demand))))

(defn overstocked-products [levels products max-days daily-demands]
  (filterv (fn [p] (let [pid (cat/product-id p)
   raw (get daily-demands pid)
   demand (if (nil? raw) 1 raw)]
  (> (days-of-stock levels pid demand) max-days))) products))
