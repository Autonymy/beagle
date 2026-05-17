(ns shipping
  (:require [catalog :as cat]
            [customers :as cust]
            [inventory :as inv]
            [orders :as ord]))

(defrecord Carrier [id name base-rate per-kg-rate max-weight active])

(defn carrier-id [r] (:id r))

(defn carrier-name [r] (:name r))

(defn carrier-base-rate [r] (:base-rate r))

(defn carrier-per-kg-rate [r] (:per-kg-rate r))

(defn carrier-max-weight [r] (:max-weight r))

(defn carrier-active [r] (:active r))

(defrecord Shipment [id order-id carrier-id warehouse-id status tracking-number weight-kg shipping-cost created-at delivered-at])

(defn shipment-id [r] (:id r))

(defn shipment-order-id [r] (:order-id r))

(defn shipment-carrier-id [r] (:carrier-id r))

(defn shipment-warehouse-id [r] (:warehouse-id r))

(defn shipment-status [r] (:status r))

(defn shipment-tracking-number [r] (:tracking-number r))

(defn shipment-weight-kg [r] (:weight-kg r))

(defn shipment-shipping-cost [r] (:shipping-cost r))

(defn shipment-created-at [r] (:created-at r))

(defn shipment-delivered-at [r] (:delivered-at r))

(defrecord DeliveryZone [id name surcharge-pct])

(defn deliveryzone-id [r] (:id r))

(defn deliveryzone-name [r] (:name r))

(defn deliveryzone-surcharge-pct [r] (:surcharge-pct r))

(defn find-carrier-by-id [carriers id]
  (first (filterv (fn [c] (= (carrier-id c) id)) carriers)))

(defn active-carriers [carriers]
  (filterv (fn [c] (carrier-active c)) carriers))

(defn carrier-total-cost [c weight-kg]
  (+ (carrier-base-rate c) (* (carrier-per-kg-rate c) weight-kg)))

(defn carriers-for-weight [carriers weight-kg]
  (filterv (fn [c] (and (carrier-active c) (>= (carrier-max-weight c) weight-kg))) carriers))

(defn cheapest-carrier [carriers weight-kg]
  (let [eligible (carriers-for-weight carriers weight-kg)]
  (if (empty? eligible) nil (reduce (fn [best c] (if (< (carrier-total-cost c weight-kg) (carrier-total-cost best weight-kg)) c best)) (first eligible) (rest eligible)))))

(defn zone-surcharge [zone cost]
  (quot (* cost (deliveryzone-surcharge-pct zone)) 100))

(defn create-shipment [id order c warehouse-id weight-kg zone]
  (let [base-cost (carrier-total-cost c weight-kg)
   surcharge (zone-surcharge zone base-cost)
   total-cost (+ base-cost surcharge)]
  (->Shipment id (ord/order-id order) (carrier-id c) warehouse-id "pending" "" weight-kg total-cost 0 0)))

(defn ship-shipment [s timestamp]
  (if (= (shipment-status s) "pending") (->Shipment (shipment-id s) (shipment-order-id s) (shipment-carrier-id s) (shipment-warehouse-id s) "shipped" (shipment-tracking-number s) (shipment-weight-kg s) (shipment-shipping-cost s) (shipment-created-at s) (shipment-delivered-at s)) s))

(defn deliver-shipment [s timestamp]
  (if (= (shipment-status s) "shipped") (->Shipment (shipment-id s) (shipment-order-id s) (shipment-carrier-id s) (shipment-warehouse-id s) "delivered" (shipment-tracking-number s) (shipment-weight-kg s) (shipment-shipping-cost s) (shipment-created-at s) timestamp) s))

(defn cancel-shipment [s]
  (if (= (shipment-status s) "pending") (->Shipment (shipment-id s) (shipment-order-id s) (shipment-carrier-id s) (shipment-warehouse-id s) "cancelled" (shipment-tracking-number s) (shipment-weight-kg s) (shipment-shipping-cost s) (shipment-created-at s) (shipment-delivered-at s)) s))

(defn find-shipment-by-id [shipments id]
  (first (filterv (fn [s] (= (shipment-id s) id)) shipments)))

(defn shipments-for-order [shipments order-id]
  (filterv (fn [s] (= (shipment-order-id s) order-id)) shipments))

(defn shipments-by-carrier [shipments carrier-id]
  (filterv (fn [s] (= (shipment-carrier-id s) carrier-id)) shipments))

(defn shipments-by-status [shipments status]
  (filterv (fn [s] (= (shipment-status s) status)) shipments))

(defn pending-shipments [shipments]
  (shipments-by-status shipments "pending"))

(defn delivered-shipments [shipments]
  (shipments-by-status shipments "delivered"))

(defn shipment-delivered? [s]
  (= (shipment-status s) "delivered"))

(defn total-shipping-revenue [shipments]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 (delivered-shipments shipments)))

(defn avg-shipping-cost [shipments]
  (let [non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) shipments)
   cnt (count non-cancelled)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 non-cancelled) cnt))))

(defn carrier-revenue [shipments carrier-id]
  (let [carrier-delivered (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (= (shipment-status s) "delivered"))) shipments)]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 carrier-delivered)))

(defn carrier-shipment-count [shipments carrier-id]
  (count (shipments-by-carrier shipments carrier-id)))

(defn carrier-on-time-count [shipments carrier-id max-days]
  (let [max-seconds (* max-days 86400)
   carrier-delivered (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (= (shipment-status s) "delivered"))) shipments)]
  (count (filterv (fn [s] (<= (- (shipment-delivered-at s) (shipment-created-at s)) max-seconds)) carrier-delivered))))

(defn carrier-on-time-pct [shipments carrier-id max-days]
  (let [carrier-delivered (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (= (shipment-status s) "delivered"))) shipments)
   total (count carrier-delivered)]
  (if (= total 0) 0 (quot (* (carrier-on-time-count shipments carrier-id max-days) 100) total))))

(defn shipping-cost-for-order [shipments order-id]
  (let [order-shipments (shipments-for-order shipments order-id)]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 order-shipments)))

(defn order-fully-shipped? [shipments order]
  (let [oid (ord/order-id order)
   order-ships (filterv (fn [s] (and (= (shipment-order-id s) oid) (not= (shipment-status s) "cancelled"))) shipments)]
  (> (count order-ships) 0)))

(defn unshipped-orders [shipments orders]
  (filterv (fn [o] (let [oid (ord/order-id o)
   matching (filterv (fn [s] (= (shipment-order-id s) oid)) shipments)]
  (= (count matching) 0))) orders))

(defn warehouse-shipment-count [shipments warehouse-id]
  (count (filterv (fn [s] (= (shipment-warehouse-id s) warehouse-id)) shipments)))

(defn set-tracking-number [s tracking]
  (->Shipment (shipment-id s) (shipment-order-id s) (shipment-carrier-id s) (shipment-warehouse-id s) (shipment-status s) tracking (shipment-weight-kg s) (shipment-shipping-cost s) (shipment-created-at s) (shipment-delivered-at s)))

(defn set-created-at [s ts]
  (->Shipment (shipment-id s) (shipment-order-id s) (shipment-carrier-id s) (shipment-warehouse-id s) (shipment-status s) (shipment-tracking-number s) (shipment-weight-kg s) (shipment-shipping-cost s) ts (shipment-delivered-at s)))

(defn delivery-time-seconds [s]
  (if (= (shipment-status s) "delivered") (- (shipment-delivered-at s) (shipment-created-at s)) 0))

(defn delivery-time-days [s]
  (quot (delivery-time-seconds s) 86400))

(defn avg-delivery-time-days [shipments]
  (let [delivered (delivered-shipments shipments)
   cnt (count delivered)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (delivery-time-days s))) 0 delivered) cnt))))

(defn fastest-delivery [shipments]
  (let [delivered (delivered-shipments shipments)]
  (if (empty? delivered) nil (reduce (fn [best s] (if (< (delivery-time-seconds s) (delivery-time-seconds best)) s best)) (first delivered) (rest delivered)))))

(defn slowest-delivery [shipments]
  (let [delivered (delivered-shipments shipments)]
  (if (empty? delivered) nil (reduce (fn [worst s] (if (> (delivery-time-seconds s) (delivery-time-seconds worst)) s worst)) (first delivered) (rest delivered)))))

(defn carrier-avg-delivery-days [shipments carrier-id]
  (let [carrier-delivered (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (= (shipment-status s) "delivered"))) shipments)
   cnt (count carrier-delivered)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (delivery-time-days s))) 0 carrier-delivered) cnt))))

(defn carrier-avg-cost [shipments carrier-id]
  (let [carrier-ships (shipments-by-carrier shipments carrier-id)
   cnt (count carrier-ships)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 carrier-ships) cnt))))

(defn rank-carriers-by-cost [carriers weight-kg]
  (let [eligible (carriers-for-weight carriers weight-kg)]
  (sort-by (fn [c] (carrier-total-cost c weight-kg)) eligible)))

(defn rank-carriers-by-revenue [carriers shipments]
  (reverse (sort-by (fn [c] (carrier-revenue shipments (carrier-id c))) carriers)))

(defn total-weight-shipped [shipments]
  (reduce (fn [acc s] (+ acc (shipment-weight-kg s))) 0 (delivered-shipments shipments)))

(defn avg-shipment-weight [shipments]
  (let [non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) shipments)
   cnt (count non-cancelled)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (shipment-weight-kg s))) 0 non-cancelled) cnt))))

(defn heaviest-shipment [shipments]
  (if (empty? shipments) nil (reduce (fn [best s] (if (> (shipment-weight-kg s) (shipment-weight-kg best)) s best)) (first shipments) (rest shipments))))

(defn lightest-shipment [shipments]
  (let [non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) shipments)]
  (if (empty? non-cancelled) nil (reduce (fn [best s] (if (< (shipment-weight-kg s) (shipment-weight-kg best)) s best)) (first non-cancelled) (rest non-cancelled)))))

(defn cost-per-kg [s]
  (let [w (shipment-weight-kg s)]
  (if (= w 0) 0 (quot (shipment-shipping-cost s) w))))

(defn avg-cost-per-kg [shipments]
  (let [delivered (delivered-shipments shipments)
   cnt (count delivered)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (cost-per-kg s))) 0 delivered) cnt))))

(defn warehouse-total-shipping-cost [shipments warehouse-id]
  (let [wh-ships (filterv (fn [s] (= (shipment-warehouse-id s) warehouse-id)) shipments)]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 wh-ships)))

(defn warehouse-avg-shipping-cost [shipments warehouse-id]
  (let [wh-ships (filterv (fn [s] (= (shipment-warehouse-id s) warehouse-id)) shipments)
   cnt (count wh-ships)]
  (if (= cnt 0) 0 (quot (warehouse-total-shipping-cost shipments warehouse-id) cnt))))

(defn warehouse-delivered-count [shipments warehouse-id]
  (count (filterv (fn [s] (and (= (shipment-warehouse-id s) warehouse-id) (= (shipment-status s) "delivered"))) shipments)))

(defn valid-shipment? [s]
  (and (> (shipment-id s) 0) (> (shipment-order-id s) 0) (> (shipment-carrier-id s) 0) (> (shipment-warehouse-id s) 0) (> (shipment-weight-kg s) 0) (>= (shipment-shipping-cost s) 0)))

(defn valid-carrier? [c]
  (and (> (carrier-id c) 0) (not= (carrier-name c) "") (>= (carrier-base-rate c) 0) (>= (carrier-per-kg-rate c) 0) (> (carrier-max-weight c) 0)))

(defn shipment-summary [s]
  (str "Shipment #" (shipment-id s) " | Order #" (shipment-order-id s) " | " (shipment-status s) " | " (cat/format-price (shipment-shipping-cost s)) " | " (shipment-weight-kg s) "kg"))

(defn carrier-summary [c]
  (str (carrier-name c) " | Base: " (cat/format-price (carrier-base-rate c)) " | Per-kg: " (cat/format-price (carrier-per-kg-rate c)) " | Max: " (carrier-max-weight c) "kg" " | " (if (carrier-active c) "active" "inactive")))

(defn zone-summary [z]
  (str (deliveryzone-name z) " (+" (deliveryzone-surcharge-pct z) "% surcharge)"))

(defn shipment-status-counts [shipments]
  (let [pending-count (count (pending-shipments shipments))
   shipped-count (count (shipments-by-status shipments "shipped"))
   delivered-count (count (delivered-shipments shipments))
   cancelled-count (count (shipments-by-status shipments "cancelled"))]
  {:pending pending-count :shipped shipped-count :delivered delivered-count :cancelled cancelled-count}))

(defn shipment-base-cost [s carriers]
  (let [c (find-carrier-by-id carriers (shipment-carrier-id s))]
  (if (nil? c) 0 (carrier-total-cost c (shipment-weight-kg s)))))

(defn shipment-surcharge-amount [s carriers]
  (let [base (shipment-base-cost s carriers)
   total (shipment-shipping-cost s)]
  (- total base)))

(defn orders-with-shipments [shipments orders]
  (filterv (fn [o] (let [oid (ord/order-id o)
   matching (filterv (fn [s] (= (shipment-order-id s) oid)) shipments)]
  (> (count matching) 0))) orders))

(defn order-shipment-count [shipments order-id]
  (count (shipments-for-order shipments order-id)))

(defn order-all-delivered? [shipments order-id]
  (let [order-ships (shipments-for-order shipments order-id)
   non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) order-ships)]
  (if (= (count non-cancelled) 0) false (every? (fn [s] (= (shipment-status s) "delivered")) non-cancelled))))

(defn carrier-total-weight [shipments carrier-id]
  (let [carrier-ships (shipments-by-carrier shipments carrier-id)]
  (reduce (fn [acc s] (+ acc (shipment-weight-kg s))) 0 carrier-ships)))

(defn carrier-utilization-pct [shipments c]
  (let [total-weight (carrier-total-weight shipments (carrier-id c))
   max-w (carrier-max-weight c)
   ship-count (carrier-shipment-count shipments (carrier-id c))]
  (if (or (= max-w 0) (= ship-count 0)) 0 (quot (* (quot total-weight ship-count) 100) max-w))))

(defn find-zone-by-id [zones id]
  (first (filterv (fn [z] (= (deliveryzone-id z) id)) zones)))

(defn find-zone-by-name [zones name]
  (first (filterv (fn [z] (= (deliveryzone-name z) name)) zones)))

(defn zone-has-surcharge? [zone]
  (> (deliveryzone-surcharge-pct zone) 0))

(defn ship-all-pending [shipments timestamp]
  (mapv (fn [s] (if (= (shipment-status s) "pending") (ship-shipment s timestamp) s)) shipments))

(defn deliver-all-shipped [shipments timestamp]
  (mapv (fn [s] (if (= (shipment-status s) "shipped") (deliver-shipment s timestamp) s)) shipments))

(defn cancel-all-pending [shipments]
  (mapv (fn [s] (if (= (shipment-status s) "pending") (cancel-shipment s) s)) shipments))

(defn estimate-shipping-cost [carriers weight-kg zone]
  (let [c (cheapest-carrier carriers weight-kg)]
  (if (nil? c) 0 (let [base (carrier-total-cost c weight-kg)
   surcharge (zone-surcharge zone base)]
  (+ base surcharge)))))

(defn estimate-shipping-cost-with-carrier [c weight-kg zone]
  (let [base (carrier-total-cost c weight-kg)
   surcharge (zone-surcharge zone base)]
  (+ base surcharge)))

(defn total-pending-cost [shipments]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 (pending-shipments shipments)))

(defn total-in-transit-cost [shipments]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 (shipments-by-status shipments "shipped")))

(defn total-cancelled-cost [shipments]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 (shipments-by-status shipments "cancelled")))

(defn shipping-kpi [shipments carriers]
  (let [total-rev (total-shipping-revenue shipments)
   total-count (count shipments)
   delivered-count (count (delivered-shipments shipments))
   pending-count (count (pending-shipments shipments))
   avg-cost (avg-shipping-cost shipments)
   avg-weight (avg-shipment-weight shipments)
   avg-delivery (avg-delivery-time-days shipments)]
  {:total-revenue total-rev :total-shipments total-count :delivered-count delivered-count :pending-count pending-count :avg-cost avg-cost :avg-weight avg-weight :avg-delivery-days avg-delivery}))

(defn sort-shipments-by-cost [shipments]
  (sort-by shipment-shipping-cost shipments))

(defn sort-shipments-by-weight [shipments]
  (sort-by shipment-weight-kg shipments))

(defn sort-shipments-by-date [shipments]
  (sort-by shipment-created-at shipments))

(defn shipments-in-period [shipments start end]
  (filterv (fn [s] (and (>= (shipment-created-at s) start) (<= (shipment-created-at s) end))) shipments))

(defn delivered-in-period [shipments start end]
  (filterv (fn [s] (and (= (shipment-status s) "delivered") (>= (shipment-delivered-at s) start) (<= (shipment-delivered-at s) end))) shipments))

(defn shipping-revenue-in-period [shipments start end]
  (let [period-delivered (delivered-in-period shipments start end)]
  (reduce (fn [acc s] (+ acc (shipment-shipping-cost s))) 0 period-delivered)))

(defn customer-shipment-count [shipments orders cust-id]
  (let [cust-order-ids (mapv (fn [o] (ord/order-id o)) (ord/orders-by-customer orders cust-id))]
  (count (filterv (fn [s] (some (fn [oid] (= (shipment-order-id s) oid)) cust-order-ids)) shipments))))

(defn customer-total-shipping-cost [shipments orders cust-id]
  (let [cust-order-ids (mapv (fn [o] (ord/order-id o)) (ord/orders-by-customer orders cust-id))]
  (reduce (fn [acc s] (if (some (fn [oid] (= (shipment-order-id s) oid)) cust-order-ids) (+ acc (shipment-shipping-cost s)) acc)) 0 shipments)))

(defn shipment-detail [s carriers orders]
  (let [c (find-carrier-by-id carriers (shipment-carrier-id s))
   carrier-name (if (nil? c) "unknown" (carrier-name c))
   o (ord/find-order-by-id orders (shipment-order-id s))
   order-total (if (nil? o) 0 (ord/order-total o))]
  (str "Shipment #" (shipment-id s) " | Carrier: " carrier-name " | Order #" (shipment-order-id s) " | Order total: " (cat/format-price order-total) " | Status: " (shipment-status s) " | Tracking: " (shipment-tracking-number s) " | Weight: " (shipment-weight-kg s) "kg" " | Cost: " (cat/format-price (shipment-shipping-cost s)))))

(defn shipment-cost-breakdown [s carriers]
  (let [c (find-carrier-by-id carriers (shipment-carrier-id s))
   base (if (nil? c) 0 (carrier-base-rate c))
   per-kg (if (nil? c) 0 (carrier-per-kg-rate c))
   weight (shipment-weight-kg s)
   weight-cost (* per-kg weight)
   carrier-cost (+ base weight-cost)
   surcharge (- (shipment-shipping-cost s) carrier-cost)]
  {:base-rate base :per-kg-rate per-kg :weight weight :weight-cost weight-cost :carrier-cost carrier-cost :surcharge surcharge :total (shipment-shipping-cost s)}))

(defn carrier-delivery-score [shipments carrier-id max-days]
  (let [on-time-pct (carrier-on-time-pct shipments carrier-id max-days)
   total-delivered (count (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (= (shipment-status s) "delivered"))) shipments))
   volume-bonus (min (* total-delivered 2) 20)]
  (+ on-time-pct volume-bonus)))

(defn carrier-cost-score [c reference-weight]
  (let [cost (carrier-total-cost c reference-weight)
   max-reasonable-cost 10000]
  (if (= max-reasonable-cost 0) 0 (max 0 (- 100 (quot (* cost 100) max-reasonable-cost))))))

(defn carrier-overall-score [shipments c max-days reference-weight]
  (let [delivery (carrier-delivery-score shipments (carrier-id c) max-days)
   cost (carrier-cost-score c reference-weight)]
  (quot (+ (* delivery 60) (* cost 40)) 100)))

(defn rank-carriers-by-score [carriers shipments max-days reference-weight]
  (let [active (active-carriers carriers)]
  (reverse (sort-by (fn [c] (carrier-overall-score shipments c max-days reference-weight)) active))))

(defn best-carrier [carriers shipments max-days reference-weight]
  (let [ranked (rank-carriers-by-score carriers shipments max-days reference-weight)]
  (first ranked)))

(defn carrier-performance-report [shipments c max-days]
  (let [cid (carrier-id c)
   total-count (carrier-shipment-count shipments cid)
   delivered-count (count (filterv (fn [s] (and (= (shipment-carrier-id s) cid) (= (shipment-status s) "delivered"))) shipments))
   on-time (carrier-on-time-count shipments cid max-days)
   on-time-pct (carrier-on-time-pct shipments cid max-days)
   avg-days (carrier-avg-delivery-days shipments cid)
   total-rev (carrier-revenue shipments cid)
   avg-cost (carrier-avg-cost shipments cid)]
  {:carrier-id cid :carrier-name (carrier-name c) :total-shipments total-count :delivered delivered-count :on-time on-time :on-time-pct on-time-pct :avg-delivery-days avg-days :total-revenue total-rev :avg-cost avg-cost}))

(defn warehouse-shipping-report [shipments warehouse-id]
  (let [wh-ships (filterv (fn [s] (= (shipment-warehouse-id s) warehouse-id)) shipments)
   total-count (count wh-ships)
   delivered-count (warehouse-delivered-count shipments warehouse-id)
   pending-count (count (filterv (fn [s] (and (= (shipment-warehouse-id s) warehouse-id) (= (shipment-status s) "pending"))) shipments))
   total-cost (warehouse-total-shipping-cost shipments warehouse-id)
   avg-cost (warehouse-avg-shipping-cost shipments warehouse-id)
   total-weight (reduce (fn [acc s] (+ acc (shipment-weight-kg s))) 0 wh-ships)
   avg-weight (if (= total-count 0) 0 (quot total-weight total-count))]
  {:warehouse-id warehouse-id :total-shipments total-count :delivered delivered-count :pending pending-count :total-cost total-cost :avg-cost avg-cost :total-weight total-weight :avg-weight avg-weight}))

(defn busiest-warehouse [shipments warehouse-ids]
  (if (empty? warehouse-ids) 0 (reduce (fn [best-id wid] (if (> (warehouse-shipment-count shipments wid) (warehouse-shipment-count shipments best-id)) wid best-id)) (first warehouse-ids) (rest warehouse-ids))))

(defn warehouse-carrier-distribution [shipments warehouse-id carriers]
  (let [wh-ships (filterv (fn [s] (= (shipment-warehouse-id s) warehouse-id)) shipments)]
  (mapv (fn [c] (let [cid (carrier-id c)
   count-for (count (filterv (fn [s] (= (shipment-carrier-id s) cid)) wh-ships))]
  {:carrier-id cid :carrier-name (carrier-name c) :shipment-count count-for})) carriers)))

(defn shipment-age-days [s now]
  (quot (- now (shipment-created-at s)) 86400))

(defn stale-shipments [shipments now max-age-days]
  (let [max-age-seconds (* max-age-days 86400)]
  (filterv (fn [s] (and (= (shipment-status s) "pending") (> (- now (shipment-created-at s)) max-age-seconds))) shipments)))

(defn stuck-shipments [shipments now max-transit-days]
  (let [max-seconds (* max-transit-days 86400)]
  (filterv (fn [s] (and (= (shipment-status s) "shipped") (> (- now (shipment-created-at s)) max-seconds))) shipments)))

(defn shipment-turnaround-days [s]
  (if (= (shipment-status s) "delivered") (quot (- (shipment-delivered-at s) (shipment-created-at s)) 86400) 0))

(defn split-weight [total-weight max-per-carrier]
  (if (<= total-weight 0) [] (if (<= total-weight max-per-carrier) [total-weight] (let [full-packages (quot total-weight max-per-carrier)
   remainder (mod total-weight max-per-carrier)
   full-list (vec (repeat full-packages max-per-carrier))]
  (if (= remainder 0) full-list (conj full-list remainder))))))

(defn estimate-multi-package-cost [c total-weight zone]
  (let [packages (split-weight total-weight (carrier-max-weight c))
   costs (mapv (fn [w] (let [base (carrier-total-cost c w)
   surcharge (zone-surcharge zone base)]
  (+ base surcharge))) packages)]
  (reduce + 0 costs)))

(defn shipping-as-pct-of-order [s orders]
  (let [o (ord/find-order-by-id orders (shipment-order-id s))
   order-total (if (nil? o) 0 (ord/order-total o))]
  (if (= order-total 0) 0 (quot (* (shipment-shipping-cost s) 100) order-total))))

(defn avg-shipping-pct-of-orders [shipments orders]
  (let [non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) shipments)
   cnt (count non-cancelled)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc s] (+ acc (shipping-as-pct-of-order s orders))) 0 non-cancelled) cnt))))

(defn high-shipping-cost-orders [shipments orders max-pct]
  (let [non-cancelled (filterv (fn [s] (not= (shipment-status s) "cancelled")) shipments)]
  (filterv (fn [s] (> (shipping-as-pct-of-order s orders) max-pct)) non-cancelled)))

(defn zone-total-surcharges [zones shipments carriers]
  (reduce (fn [acc s] (+ acc (shipment-surcharge-amount s carriers))) 0 shipments))

(defn zone-count [zones]
  (count zones))

(defn zones-with-surcharge [zones]
  (filterv zone-has-surcharge? zones))

(defn max-surcharge-zone [zones]
  (if (empty? zones) nil (reduce (fn [best z] (if (> (deliveryzone-surcharge-pct z) (deliveryzone-surcharge-pct best)) z best)) (first zones) (rest zones))))

(defn shipment-count-in-period [shipments start end]
  (count (shipments-in-period shipments start end)))

(defn carrier-volume-in-period [shipments carrier-id start end]
  (count (filterv (fn [s] (and (= (shipment-carrier-id s) carrier-id) (>= (shipment-created-at s) start) (<= (shipment-created-at s) end))) shipments)))

(defn shipping-report [shipments carriers orders zones]
  (let [total-count (count shipments)
   del-count (count (delivered-shipments shipments))
   pend-count (count (pending-shipments shipments))
   shipped-count (count (shipments-by-status shipments "shipped"))
   canc-count (count (shipments-by-status shipments "cancelled"))
   total-rev (total-shipping-revenue shipments)
   avg-cost (avg-shipping-cost shipments)
   avg-weight (avg-shipment-weight shipments)
   avg-delivery (avg-delivery-time-days shipments)
   total-weight (total-weight-shipped shipments)
   avg-cpk (avg-cost-per-kg shipments)
   active-carr (count (active-carriers carriers))
   total-zones (zone-count zones)]
  {:total-shipments total-count :delivered del-count :pending pend-count :shipped shipped-count :cancelled canc-count :total-revenue total-rev :avg-cost avg-cost :avg-weight avg-weight :avg-delivery-days avg-delivery :total-weight total-weight :avg-cost-per-kg avg-cpk :active-carriers active-carr :zone-count total-zones}))
