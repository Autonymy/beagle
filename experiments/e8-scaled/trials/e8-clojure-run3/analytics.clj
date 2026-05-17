(ns analytics
  (:require [orders :as ord]
            [inventory :as inv]
            [catalog :as cat]
            [billing :refer :all]
            [shipping :as ship]))

(defrecord PeriodMetric [period metric-name value])

(defn periodmetric-period [r] (:period r))

(defn periodmetric-metric-name [r] (:metric-name r))

(defn periodmetric-value [r] (:value r))

(defrecord TrendResult [metric-name direction change-pct])

(defn trendresult-metric-name [r] (:metric-name r))

(defn trendresult-direction [r] (:direction r))

(defn trendresult-change-pct [r] (:change-pct r))

(defrecord Bucket [label count total])

(defn bucket-label [r] (:label r))

(defn bucket-count [r] (:count r))

(defn bucket-total [r] (:total r))

(defn make-period-metric [period metric-name value]
  (->PeriodMetric period metric-name value))

(defn metrics-for-period [metrics period]
  (filterv (fn [m] (= (periodmetric-period m) period)) metrics))

(defn metric-values [metrics metric-name]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   sorted (sort-by periodmetric-period matching)]
  (mapv periodmetric-value sorted)))

(defn latest-metric [metrics metric-name]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   sorted (sort-by periodmetric-period matching)]
  (if (empty? sorted) nil (last sorted))))

(defn earliest-metric [metrics metric-name]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   sorted (sort-by periodmetric-period matching)]
  (if (empty? sorted) nil (first sorted))))

(defn metric-count [metrics metric-name]
  (count (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)))

(defn metric-sum [metrics metric-name]
  (reduce (fn [acc v] (+ acc v)) 0 (metric-values metrics metric-name)))

(defn metric-avg [metrics metric-name]
  (let [vals (metric-values metrics metric-name)
   cnt (count vals)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc v] (+ acc v)) 0 vals) cnt))))

(defn metric-max [metrics metric-name]
  (let [vals (metric-values metrics metric-name)]
  (if (empty? vals) 0 (reduce (fn [best v] (if (> v best) v best)) (first vals) (rest vals)))))

(defn metric-min [metrics metric-name]
  (let [vals (metric-values metrics metric-name)]
  (if (empty? vals) 0 (reduce (fn [best v] (if (< v best) v best)) (first vals) (rest vals)))))

(defn all-metric-names [metrics]
  (distinct (mapv periodmetric-metric-name metrics)))

(defn all-periods [metrics]
  (distinct (mapv periodmetric-period metrics)))

(defn revenue-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   rev (ord/total-revenue period-orders)]
  (->PeriodMetric label "revenue" rev))) periods))

(defn order-count-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   cnt (count period-orders)]
  (->PeriodMetric label "order-count" cnt))) periods))

(defn avg-order-value-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   avg-val (ord/avg-order-value period-orders)]
  (->PeriodMetric label "avg-order-value" avg-val))) periods))

(defn discount-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   disc (ord/total-discounts-given period-orders)]
  (->PeriodMetric label "discounts" disc))) periods))

(defn tax-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   tx (ord/total-tax-collected period-orders)]
  (->PeriodMetric label "tax" tx))) periods))

(defn units-by-period [orders periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-orders (ord/orders-in-period orders start end)
   units (ord/total-units-ordered period-orders)]
  (->PeriodMetric label "units-ordered" units))) periods))

(defn inventory-value-metric [levels products label]
  (let [val (inv/total-inventory-value levels products)]
  (->PeriodMetric label "inventory-value" val)))

(defn low-stock-count-metric [levels label]
  (let [cnt (count (inv/low-stock-items levels))]
  (->PeriodMetric label "low-stock-count" cnt)))

(defn out-of-stock-count-metric [levels label]
  (let [cnt (count (inv/out-of-stock-items levels))]
  (->PeriodMetric label "out-of-stock-count" cnt)))

(defn retail-value-metric [levels products label]
  (let [val (inv/stock-retail-value levels products)]
  (->PeriodMetric label "retail-value" val)))

(defn inventory-snapshot [levels products label]
  [(inventory-value-metric levels products label) (retail-value-metric levels products label) (low-stock-count-metric levels label) (out-of-stock-count-metric levels label)])

(defn collection-by-period [payments invoices periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-payments (filterv (fn [pay] (and (= (payment-status pay) "completed") (>= (payment-processed-at pay) start) (<= (payment-processed-at pay) end))) payments)
   total (reduce (fn [acc pay] (+ acc (payment-amount pay))) 0 period-payments)]
  (->PeriodMetric label "collections" total))) periods))

(defn outstanding-by-period [invoices payments periods]
  (mapv (fn [p] (let [label (nth p 2)
   outstanding (total-outstanding invoices payments)]
  (->PeriodMetric label "outstanding" outstanding))) periods))

(defn invoiced-by-period [invoices periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-invoices (filterv (fn [inv] (and (>= (invoice-issued-at inv) start) (<= (invoice-issued-at inv) end))) invoices)
   total (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 period-invoices)]
  (->PeriodMetric label "invoiced" total))) periods))

(defn overdue-count-by-period [invoices periods]
  (mapv (fn [p] (let [end (second p)
   label (nth p 2)
   overdue (overdue-invoices invoices end)
   cnt (count overdue)]
  (->PeriodMetric label "overdue-count" cnt))) periods))

(defn collection-rate-by-period [invoices payments periods]
  (mapv (fn [p] (let [label (nth p 2)
   rate (collection-rate-pct invoices payments)]
  (->PeriodMetric label "collection-rate" rate))) periods))

(defn shipping-cost-by-period [shipments periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-delivered (filterv (fn [s] (and (= (ship/shipment-status s) "delivered") (>= (ship/shipment-created-at s) start) (<= (ship/shipment-created-at s) end))) shipments)
   total (reduce (fn [acc s] (+ acc (ship/shipment-shipping-cost s))) 0 period-delivered)]
  (->PeriodMetric label "shipping-cost" total))) periods))

(defn delivery-count-by-period [shipments periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-delivered (filterv (fn [s] (and (= (ship/shipment-status s) "delivered") (>= (ship/shipment-created-at s) start) (<= (ship/shipment-created-at s) end))) shipments)
   cnt (count period-delivered)]
  (->PeriodMetric label "deliveries" cnt))) periods))

(defn shipment-count-by-period [shipments periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-ships (filterv (fn [s] (and (not= (ship/shipment-status s) "cancelled") (>= (ship/shipment-created-at s) start) (<= (ship/shipment-created-at s) end))) shipments)
   cnt (count period-ships)]
  (->PeriodMetric label "shipment-count" cnt))) periods))

(defn avg-shipping-cost-by-period [shipments periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-ships (filterv (fn [s] (and (not= (ship/shipment-status s) "cancelled") (>= (ship/shipment-created-at s) start) (<= (ship/shipment-created-at s) end))) shipments)
   cnt (count period-ships)
   total (reduce (fn [acc s] (+ acc (ship/shipment-shipping-cost s))) 0 period-ships)
   avg-cost (if (= cnt 0) 0 (quot total cnt))]
  (->PeriodMetric label "avg-shipping-cost" avg-cost))) periods))

(defn total-weight-by-period [shipments periods]
  (mapv (fn [p] (let [start (first p)
   end (second p)
   label (nth p 2)
   period-delivered (filterv (fn [s] (and (= (ship/shipment-status s) "delivered") (>= (ship/shipment-created-at s) start) (<= (ship/shipment-created-at s) end))) shipments)
   total-wt (reduce (fn [acc s] (+ acc (ship/shipment-weight-kg s))) 0 period-delivered)]
  (->PeriodMetric label "total-weight" total-wt))) periods))

(defn calculate-trend [metrics metric-name]
  (let [vals (metric-values metrics metric-name)]
  (if (< (count vals) 2) (->TrendResult metric-name "flat" 0) (let [first-val (first vals)
   last-val (last vals)]
  (cond
  (= first-val last-val) (->TrendResult metric-name "flat" 0)
  (> last-val first-val) (let [change (if (= first-val 0) 0 (quot (* (- last-val first-val) 100) first-val))]
  (->TrendResult metric-name "up" change))
  true (let [change (if (= first-val 0) 0 (quot (* (- first-val last-val) 100) first-val))]
  (->TrendResult metric-name "down" change)))))))

(defn compare-periods [metrics metric-name period1 period2]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   m1 (first (filterv (fn [m] (= (periodmetric-period m) period1)) matching))
   m2 (first (filterv (fn [m] (= (periodmetric-period m) period2)) matching))
   v1 (if (nil? m1) 0 (periodmetric-value m1))
   v2 (if (nil? m2) 0 (periodmetric-value m2))]
  (cond
  (= v1 v2) (->TrendResult metric-name "flat" 0)
  (> v2 v1) (let [change (if (= v1 0) 0 (quot (* (- v2 v1) 100) v1))]
  (->TrendResult metric-name "up" change))
  true (let [change (if (= v1 0) 0 (quot (* (- v1 v2) 100) v1))]
  (->TrendResult metric-name "down" change)))))

(defn trend-direction [tr]
  (trendresult-direction tr))

(defn trend-is-positive? [tr]
  (= (trendresult-direction tr) "up"))

(defn trend-is-negative? [tr]
  (= (trendresult-direction tr) "down"))

(defn trend-magnitude [tr]
  (trendresult-change-pct tr))

(defn trend-summary [tr]
  (let [name (trendresult-metric-name tr)
   dir (trendresult-direction tr)
   pct (trendresult-change-pct tr)]
  (cond
  (= dir "flat") (str name ": flat (no change)")
  (= dir "up") (str name ": up " pct "%")
  true (str name ": down " pct "%"))))

(defn multi-trend [metrics metric-names]
  (mapv (fn [name] (calculate-trend metrics name)) metric-names))

(defn bucket-orders-by-value [orders thresholds]
  (mapv (fn [t] (let [label (first t)
   lo (second t)
   hi (nth t 2)
   matching (filterv (fn [o] (and (>= (ord/order-total o) lo) (<= (ord/order-total o) hi))) orders)
   cnt (count matching)
   total (reduce (fn [acc o] (+ acc (ord/order-id o))) 0 matching)]
  (->Bucket label cnt total))) thresholds))

(defn bucket-customers-by-spend [customers orders thresholds]
  (mapv (fn [t] (let [label (first t)
   lo (second t)
   hi (nth t 2)
   results (reduce (fn [acc cid] (let [spend (ord/customer-total-spend orders cid)]
  (if (and (>= spend lo) (<= spend hi)) {:count (+ (:count acc) 1) :total (+ (:total acc) spend)} acc))) {:count 0 :total 0} customers)]
  (->Bucket label (:count results) (:total results)))) thresholds))

(defn sum-bucket-totals [buckets]
  (reduce (fn [acc b] (+ acc (bucket-total b))) 0 buckets))

(defn sum-bucket-counts [buckets]
  (reduce (fn [acc b] (+ acc (bucket-count b))) 0 buckets))

(defn largest-bucket [buckets]
  (if (empty? buckets) nil (reduce (fn [best b] (if (> (bucket-total b) (bucket-total best)) b best)) (first buckets) (rest buckets))))

(defn most-populated-bucket [buckets]
  (if (empty? buckets) nil (reduce (fn [best b] (if (> (bucket-count b) (bucket-count best)) b best)) (first buckets) (rest buckets))))

(defn bucket-summary [b]
  (str (bucket-label b) ": " (bucket-count b) " items" ", total " (cat/format-price (bucket-total b))))

(defn bucket-distribution [buckets]
  (let [grand (reduce (fn [acc b] (+ acc (bucket-total b))) 0 buckets)]
  (mapv (fn [b] (let [pct (if (= grand 0) 0 (quot (* (bucket-total b) 100) grand))]
  {:label (bucket-label b) :count (bucket-count b) :total (bucket-total b) :pct pct})) buckets)))

(defn gross-margin-metric [orders label]
  (let [rev (ord/total-revenue orders)
   disc (ord/total-discounts-given orders)
   margin (- rev disc)]
  (->PeriodMetric label "gross-margin" margin)))

(defn shipping-to-revenue-pct [orders shipments]
  (let [rev (ord/total-revenue orders)
   ship-cost (ship/total-shipping-revenue shipments)]
  (if (= rev 0) 0 (quot (* ship-cost 100) rev))))

(defn inventory-turnover-ratio [orders levels products]
  (let [rev (ord/total-revenue orders)
   inv-val (inv/total-inventory-value levels products)]
  (if (= inv-val 0) 0 (quot (* rev 100) inv-val))))

(defn revenue-per-order-line [orders]
  (let [rev (ord/total-revenue orders)
   lines (ord/total-line-count orders)]
  (if (= lines 0) 0 (quot rev lines))))

(defn order-size-distribution [orders small-max large-min]
  (let [small-cnt (count (filterv (fn [o] (<= (ord/order-total o) small-max)) orders))
   large-cnt (count (filterv (fn [o] (>= (ord/order-total o) large-min)) orders))
   medium-cnt (count (filterv (fn [o] (and (> (ord/order-total o) small-max) (< (ord/order-total o) large-min))) orders))]
  {:small small-cnt :medium medium-cnt :large large-cnt}))

(defn kpi-dashboard [orders invoices payments shipments levels products]
  (let [total-rev (ord/total-revenue orders)
   total-collected (total-revenue-collected payments)
   total-out (total-outstanding invoices payments)
   total-ship (ship/total-shipping-revenue shipments)
   inv-val (inv/total-inventory-value levels products)
   order-cnt (count orders)]
  {:total-revenue total-rev :total-collected total-collected :total-outstanding total-out :total-shipping total-ship :inventory-value inv-val :order-count order-cnt}))

(defn extended-kpi-dashboard [orders invoices payments shipments levels products]
  (let [base (kpi-dashboard orders invoices payments shipments levels products)
   avg-order (ord/avg-order-value orders)
   total-lines (ord/total-line-count orders)
   total-units (ord/total-units-ordered orders)
   total-disc (ord/total-discounts-given orders)
   total-tax (ord/total-tax-collected orders)
   pending-cnt (count (ord/pending-orders orders))
   active-cnt (count (ord/active-orders orders))
   low-stock-cnt (count (inv/low-stock-items levels))
   oos-cnt (count (inv/out-of-stock-items levels))
   ship-rev (ship/total-shipping-revenue shipments)
   avg-ship (ship/avg-shipping-cost shipments)
   avg-delivery (ship/avg-delivery-time-days shipments)
   coll-rate (collection-rate-pct invoices payments)
   overdue-cnt (count (overdue-invoices invoices 0))
   net-rev (net-revenue payments)]
  {:total-revenue (:total-revenue base) :total-collected (:total-collected base) :total-outstanding (:total-outstanding base) :total-shipping (:total-shipping base) :inventory-value (:inventory-value base) :order-count (:order-count base) :avg-order-value avg-order :total-line-items total-lines :total-units-ordered total-units :total-discounts total-disc :total-tax total-tax :pending-orders pending-cnt :active-orders active-cnt :low-stock-count low-stock-cnt :out-of-stock-count oos-cnt :avg-shipping-cost avg-ship :avg-delivery-days avg-delivery :collection-rate coll-rate :overdue-invoices overdue-cnt :net-revenue net-rev}))

(defn period-over-period [metrics metric-name period1 period2]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   m1 (first (filterv (fn [m] (= (periodmetric-period m) period1)) matching))
   m2 (first (filterv (fn [m] (= (periodmetric-period m) period2)) matching))
   v1 (if (nil? m1) 0 (periodmetric-value m1))
   v2 (if (nil? m2) 0 (periodmetric-value m2))
   abs-change (- v2 v1)
   pct-change (if (= v1 0) 0 (quot (* abs-change 100) v1))]
  {:metric metric-name :period1 period1 :period2 period2 :value1 v1 :value2 v2 :change abs-change :change-pct pct-change}))

(defn generate-all-metrics [orders periods]
  (let [rev (revenue-by-period orders periods)
   cnt (order-count-by-period orders periods)
   avg-val (avg-order-value-by-period orders periods)
   disc (discount-by-period orders periods)
   tx (tax-by-period orders periods)
   units (units-by-period orders periods)]
  (into [] (concat rev cnt avg-val disc tx units))))

(defn format-metric [m]
  (str "[" (periodmetric-period m) "] " (periodmetric-metric-name m) " = " (periodmetric-value m)))

(defn format-trend [tr]
  (str (trendresult-metric-name tr) ": " (trendresult-direction tr) " " (trendresult-change-pct tr) "%"))

(defn format-bucket [b]
  (str (bucket-label b) ": " (bucket-count b) " items, " (cat/format-price (bucket-total b))))

(defn valid-period-metric? [m]
  (and (not= (periodmetric-period m) "") (not= (periodmetric-metric-name m) "")))

(defn valid-trend-result? [tr]
  (and (not= (trendresult-metric-name tr) "") (or (= (trendresult-direction tr) "up") (= (trendresult-direction tr) "down") (= (trendresult-direction tr) "flat")) (>= (trendresult-change-pct tr) 0)))

(defn valid-bucket? [b]
  (and (not= (bucket-label b) "") (>= (bucket-count b) 0) (>= (bucket-total b) 0)))

(defn sort-metrics-by-value [metrics]
  (sort-by periodmetric-value metrics))

(defn sort-metrics-by-period [metrics]
  (sort-by periodmetric-period metrics))

(defn sort-buckets-by-total [buckets]
  (sort-by bucket-total buckets))

(defn sort-buckets-by-count [buckets]
  (sort-by bucket-count buckets))

(defn top-metrics [metrics metric-name n]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   sorted (reverse (sort-by periodmetric-value matching))]
  (vec (take n sorted))))

(defn bottom-metrics [metrics metric-name n]
  (let [matching (filterv (fn [m] (= (periodmetric-metric-name m) metric-name)) metrics)
   sorted (sort-by periodmetric-value matching)]
  (vec (take n sorted))))

(defn analytics-summary [orders invoices payments shipments levels products periods]
  (let [rev-metrics (revenue-by-period orders periods)
   cnt-metrics (order-count-by-period orders periods)
   coll-metrics (collection-by-period payments invoices periods)
   ship-metrics (shipping-cost-by-period shipments periods)
   del-metrics (delivery-count-by-period shipments periods)
   rev-trend (calculate-trend rev-metrics "revenue")
   cnt-trend (calculate-trend cnt-metrics "order-count")
   coll-trend (calculate-trend coll-metrics "collections")
   ship-trend (calculate-trend ship-metrics "shipping-cost")
   kpi (kpi-dashboard orders invoices payments shipments levels products)]
  {:kpi kpi :revenue-trend (trendresult-direction rev-trend) :revenue-change-pct (trendresult-change-pct rev-trend) :order-count-trend (trendresult-direction cnt-trend) :order-count-change-pct (trendresult-change-pct cnt-trend) :collections-trend (trendresult-direction coll-trend) :collections-change-pct (trendresult-change-pct coll-trend) :shipping-trend (trendresult-direction ship-trend) :shipping-change-pct (trendresult-change-pct ship-trend) :revenue-metrics rev-metrics :order-count-metrics cnt-metrics :collection-metrics coll-metrics :shipping-cost-metrics ship-metrics :delivery-metrics del-metrics}))
