(ns orders
  (:require [catalog :as cat]
            [inventory :as inv]
            [customers :as cust]))

(defrecord OrderLine [product-id quantity unit-price line-total])

(defn orderline-product-id [r] (:product-id r))

(defn orderline-quantity [r] (:quantity r))

(defn orderline-unit-price [r] (:unit-price r))

(defn orderline-line-total [r] (:line-total r))

(defrecord Order [id customer-id status lines subtotal tax discount total created-at])

(defn order-id [r] (:id r))

(defn order-customer-id [r] (:customer-id r))

(defn order-status [r] (:status r))

(defn order-lines [r] (:lines r))

(defn order-subtotal [r] (:subtotal r))

(defn order-tax [r] (:tax r))

(defn order-discount [r] (:discount r))

(defn order-total [r] (:total r))

(defn order-created-at [r] (:created-at r))

(defn make-order-line [products prod-id qty]
  (let [prod (cat/find-product-by-id products prod-id)
   price (if (nil? prod) 0 (cat/product-unit-price prod))
   total (* price qty)]
  (->OrderLine prod-id qty price total)))

(defn order-line-product-name [ol products]
  (let [prod (cat/find-product-by-id products (orderline-product-id ol))]
  (if (nil? prod) "unknown" (cat/product-name prod))))

(defn calculate-subtotal [lines]
  (reduce (fn [acc ol] (+ acc (orderline-line-total ol))) 0 lines))

(defn calculate-tax [subtotal tax-rate]
  (long (* (double subtotal) tax-rate)))

(defn calculate-discount [subtotal discount-pct]
  (quot (* subtotal discount-pct) 100))

(defn calculate-total [subtotal tax discount]
  (- (+ subtotal tax) discount))

(defn customer-discount-pct [customers cust-id]
  (let [c (cust/find-customer-by-id customers cust-id)]
  (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-tier c)))))

(defn create-order [id cust-id line-specs products categories customers timestamp]
  (let [lines (mapv (fn [spec] (make-order-line products (first spec) (second spec))) line-specs)
   subtotal (calculate-subtotal lines)
   disc-pct (customer-discount-pct customers cust-id)
   discount (calculate-discount subtotal disc-pct)
   tax (calculate-tax (- subtotal discount) 0.1)
   total (calculate-total subtotal tax discount)]
  (->Order id cust-id "pending" lines subtotal tax discount total timestamp)))

(def status-pending "pending")

(def status-confirmed "confirmed")

(def status-shipped "shipped")

(def status-delivered "delivered")

(def status-cancelled "cancelled")

(defn update-status [o new-status]
  (->Order (order-id o) (order-customer-id o) new-status (order-lines o) (order-subtotal o) (order-tax o) (order-discount o) (order-total o) (order-created-at o)))

(defn confirm-order [o]
  (update-status o "confirmed"))

(defn ship-order [o]
  (update-status o "shipped"))

(defn deliver-order [o]
  (update-status o "delivered"))

(defn cancel-order [o]
  (update-status o "cancelled"))

(defn order-pending? [o]
  (= (order-status o) "pending"))

(defn order-active? [o]
  (and (not= (order-status o) "cancelled") (not= (order-status o) "delivered")))

(defn can-fulfill-line? [levels ol]
  (inv/can-fulfill? levels (orderline-product-id ol) (orderline-quantity ol)))

(defn can-fulfill-order? [levels o]
  (every? (fn [ol] (can-fulfill-line? levels ol)) (order-lines o)))

(defn unfulfillable-lines [levels o]
  (filterv (fn [ol] (not (can-fulfill-line? levels ol))) (order-lines o)))

(defn fulfillment-shortage [levels ol]
  (let [avail (inv/available-quantity levels (orderline-product-id ol))
   needed (orderline-quantity ol)]
  (if (>= avail needed) 0 (- needed avail))))

(defn orders-by-customer [orders cust-id]
  (filterv (fn [o] (= (order-customer-id o) cust-id)) orders))

(defn orders-by-status [orders status]
  (filterv (fn [o] (= (order-status o) status)) orders))

(defn pending-orders [orders]
  (orders-by-status orders "pending"))

(defn active-orders [orders]
  (filterv order-active? orders))

(defn orders-above-total [orders min-total]
  (filterv (fn [o] (>= (order-total o) min-total)) orders))

(defn find-order-by-id [orders id]
  (first (filterv (fn [o] (= (order-id o) id)) orders)))

(defn sort-by-total [orders]
  (sort-by order-total orders))

(defn sort-by-date [orders]
  (sort-by order-created-at orders))

(defn total-revenue [orders]
  (reduce (fn [acc o] (+ acc (order-total o))) 0 orders))

(defn total-discounts-given [orders]
  (reduce (fn [acc o] (+ acc (order-discount o))) 0 orders))

(defn total-tax-collected [orders]
  (reduce (fn [acc o] (+ acc (order-tax o))) 0 orders))

(defn avg-order-value [orders]
  (let [cnt (count orders)]
  (if (= cnt 0) 0 (quot (total-revenue orders) cnt))))

(defn order-count-by-status [orders status]
  (count (orders-by-status orders status)))

(defn largest-order [orders]
  (reduce (fn [best o] (if (> (order-total o) (order-total best)) o best)) (first orders) (rest orders)))

(defn smallest-order [orders]
  (reduce (fn [best o] (if (< (order-total o) (order-total best)) o best)) (first orders) (rest orders)))

(defn customer-order-count [orders cust-id]
  (count (orders-by-customer orders cust-id)))

(defn customer-total-spend [orders cust-id]
  (total-revenue (orders-by-customer orders cust-id)))

(defn customer-avg-order [orders cust-id]
  (avg-order-value (orders-by-customer orders cust-id)))

(defn total-line-count [orders]
  (reduce (fn [acc o] (+ acc (count (order-lines o)))) 0 orders))

(defn total-units-ordered [orders]
  (reduce (fn [acc o] (+ acc (reduce (fn [a ol] (+ a (orderline-quantity ol))) 0 (order-lines o)))) 0 orders))

(defn product-units-ordered [orders prod-id]
  (reduce (fn [acc o] (+ acc (reduce (fn [a ol] (if (= (orderline-product-id ol) prod-id) (+ a (orderline-quantity ol)) a)) 0 (order-lines o)))) 0 orders))

(defn product-revenue [orders prod-id]
  (reduce (fn [acc o] (+ acc (reduce (fn [a ol] (if (= (orderline-product-id ol) prod-id) (+ a (orderline-line-total ol)) a)) 0 (order-lines o)))) 0 orders))

(defn order-summary [o]
  (str "Order #" (order-id o) " | " (order-status o) " | " (cat/format-price (order-total o)) " (" (count (order-lines o)) " items)"))

(defn order-line-summary [ol products]
  (str (order-line-product-name ol products) " x" (orderline-quantity ol) " @ " (cat/format-price (orderline-unit-price ol)) " = " (cat/format-price (orderline-line-total ol))))

(defn valid-order? [o]
  (and (> (order-id o) 0) (> (order-customer-id o) 0) (> (count (order-lines o)) 0) (>= (order-total o) 0)))

(defn valid-order-line? [ol]
  (and (> (orderline-product-id ol) 0) (> (orderline-quantity ol) 0) (> (orderline-unit-price ol) 0)))

(defrecord ReturnRequest [id order-id product-id quantity reason status])

(defn returnrequest-id [r] (:id r))

(defn returnrequest-order-id [r] (:order-id r))

(defn returnrequest-product-id [r] (:product-id r))

(defn returnrequest-quantity [r] (:quantity r))

(defn returnrequest-reason [r] (:reason r))

(defn returnrequest-status [r] (:status r))

(defn create-return [id order-id prod-id qty reason]
  (->ReturnRequest id order-id prod-id qty reason "pending"))

(defn approve-return [r]
  (->ReturnRequest (returnrequest-id r) (returnrequest-order-id r) (returnrequest-product-id r) (returnrequest-quantity r) (returnrequest-reason r) "approved"))

(defn deny-return [r]
  (->ReturnRequest (returnrequest-id r) (returnrequest-order-id r) (returnrequest-product-id r) (returnrequest-quantity r) (returnrequest-reason r) "denied"))

(defn return-refund-amount [r products]
  (let [prod (cat/find-product-by-id products (returnrequest-product-id r))
   price (if (nil? prod) 0 (cat/product-unit-price prod))]
  (* price (returnrequest-quantity r))))

(defn returns-for-order [returns order-id]
  (filterv (fn [r] (= (returnrequest-order-id r) order-id)) returns))

(defn approved-returns [returns]
  (filterv (fn [r] (= (returnrequest-status r) "approved")) returns))

(defn total-return-value [returns products]
  (reduce (fn [acc r] (+ acc (return-refund-amount r products))) 0 (approved-returns returns)))

(defn partial-fulfill-order [levels o]
  (let [lines (order-lines o)]
  (mapv (fn [ol] (let [avail (inv/available-quantity levels (orderline-product-id ol))
   can-ship (min (orderline-quantity ol) avail)]
  {:product-id (orderline-product-id ol) :requested (orderline-quantity ol) :shipping can-ship :backordered (- (orderline-quantity ol) can-ship)})) lines)))

(defn partial-fulfillment-value [levels o]
  (let [parts (partial-fulfill-order levels o)]
  (reduce (fn [acc part] (let [prod-id (:product-id part)
   qty (:shipping part)
   ol (first (filterv (fn [l] (= (orderline-product-id l) prod-id)) (order-lines o)))
   price (if (nil? ol) 0 (orderline-unit-price ol))]
  (+ acc (* price qty)))) 0 parts)))

(defn add-line-to-order [o new-line]
  (let [new-lines (conj (order-lines o) new-line)
   new-subtotal (calculate-subtotal new-lines)
   disc (order-discount o)
   tax (calculate-tax (- new-subtotal disc) 0.1)
   total (calculate-total new-subtotal tax disc)]
  (->Order (order-id o) (order-customer-id o) (order-status o) new-lines new-subtotal tax disc total (order-created-at o))))

(defn remove-line-from-order [o prod-id]
  (let [new-lines (filterv (fn [ol] (not= (orderline-product-id ol) prod-id)) (order-lines o))
   new-subtotal (calculate-subtotal new-lines)
   disc (order-discount o)
   tax (calculate-tax (- new-subtotal disc) 0.1)
   total (calculate-total new-subtotal tax disc)]
  (->Order (order-id o) (order-customer-id o) (order-status o) new-lines new-subtotal tax disc total (order-created-at o))))

(defn order-product-ids [o]
  (mapv orderline-product-id (order-lines o)))

(defn repeat-order [o new-id new-timestamp]
  (->Order new-id (order-customer-id o) "pending" (order-lines o) (order-subtotal o) (order-tax o) (order-discount o) (order-total o) new-timestamp))

(defn customer-reorder-rate [orders cust-id]
  (let [cust-orders (orders-by-customer orders cust-id)
   cnt (count cust-orders)]
  (if (<= cnt 1) 0 (let [all-prods (mapv order-product-ids cust-orders)
   first-prods (first all-prods)
   repeat-count (count (filterv (fn [prods] (some (fn [pid] (some (fn [fid] (= pid fid)) first-prods)) prods)) (rest all-prods)))]
  (quot (* repeat-count 100) (- cnt 1))))))

(defn orders-in-period [orders start-ts end-ts]
  (filterv (fn [o] (and (>= (order-created-at o) start-ts) (<= (order-created-at o) end-ts))) orders))

(defn revenue-in-period [orders start-ts end-ts]
  (total-revenue (orders-in-period orders start-ts end-ts)))

(defn order-frequency [orders cust-id]
  (customer-order-count orders cust-id))

(defn high-value-orders [orders threshold]
  (orders-above-total orders threshold))

(defn average-items-per-order [orders]
  (let [cnt (count orders)]
  (if (= cnt 0) 0 (quot (total-line-count orders) cnt))))
