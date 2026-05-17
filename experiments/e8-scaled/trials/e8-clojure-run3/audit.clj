(ns audit
  (:require [catalog :as cat]
            [customers :as cust]
            [inventory :as inv]
            [orders :as ord]
            [reports :as rep]
            [shipping :as ship]
            [billing :refer :all]
            [procurement :as proc]
            [promotions :as promo]
            [employees :as emp]
            [analytics :as analytics]
            [notifications :as notif]))

(defrecord AuditEntry [id entity-type entity-id action actor-id timestamp detail])

(defn auditentry-id [r] (:id r))

(defn auditentry-entity-type [r] (:entity-type r))

(defn auditentry-entity-id [r] (:entity-id r))

(defn auditentry-action [r] (:action r))

(defn auditentry-actor-id [r] (:actor-id r))

(defn auditentry-timestamp [r] (:timestamp r))

(defn auditentry-detail [r] (:detail r))

(defrecord ReconciliationResult [period expected actual discrepancy status])

(defn reconciliationresult-period [r] (:period r))

(defn reconciliationresult-expected [r] (:expected r))

(defn reconciliationresult-actual [r] (:actual r))

(defn reconciliationresult-discrepancy [r] (:discrepancy r))

(defn reconciliationresult-status [r] (:status r))

(defrecord ComplianceCheck [id check-type entity-id passed detail checked-at])

(defn compliancecheck-id [r] (:id r))

(defn compliancecheck-check-type [r] (:check-type r))

(defn compliancecheck-entity-id [r] (:entity-id r))

(defn compliancecheck-passed [r] (:passed r))

(defn compliancecheck-detail [r] (:detail r))

(defn compliancecheck-checked-at [r] (:checked-at r))

(defn create-audit-entry [id entity-type entity-id action actor-id timestamp detail]
  (->AuditEntry id entity-type entity-id action actor-id timestamp detail))

(defn audit-order [id order actor-id action timestamp]
  (->AuditEntry id "order" (ord/order-id order) action actor-id timestamp (ord/order-status order)))

(defn audit-shipment [id shipment actor-id action timestamp]
  (->AuditEntry id "shipment" (ship/shipment-id shipment) action actor-id timestamp (ship/shipment-status shipment)))

(defn audit-invoice [id invoice actor-id action timestamp]
  (->AuditEntry id "invoice" (invoice-id invoice) action actor-id timestamp (invoice-status invoice)))

(defn audit-product [id product actor-id action timestamp]
  (->AuditEntry id "product" (cat/product-id product) action actor-id timestamp (cat/product-name product)))

(defn entries-for-entity [entries entity-type entity-id]
  (filterv (fn [e] (and (= (auditentry-entity-type e) entity-type) (= (auditentry-entity-id e) entity-id))) entries))

(defn entries-by-actor [entries actor-id]
  (filterv (fn [e] (= (auditentry-actor-id e) actor-id)) entries))

(defn entries-by-action [entries action]
  (filterv (fn [e] (= (auditentry-action e) action)) entries))

(defn entries-in-period [entries start end]
  (filterv (fn [e] (and (>= (auditentry-timestamp e) start) (<= (auditentry-timestamp e) end))) entries))

(defn entry-count-by-type [entries entity-type]
  (count (filterv (fn [e] (= (auditentry-entity-type e) entity-type)) entries)))

(defn recent-entries [entries n]
  (let [sorted (reverse (sort-by auditentry-timestamp entries))]
  (vec (take n sorted))))

(defn actor-action-count [entries actor-id]
  (count (entries-by-actor entries actor-id)))

(defn entries-by-entity-type [entries entity-type]
  (filterv (fn [e] (= (auditentry-entity-type e) entity-type)) entries))

(defn distinct-actors [entries]
  (distinct (mapv auditentry-actor-id entries)))

(defn distinct-entity-types [entries]
  (distinct (mapv auditentry-entity-type entries)))

(defn entries-for-actor-in-period [entries actor-id start end]
  (filterv (fn [e] (and (= (auditentry-actor-id e) actor-id) (>= (auditentry-timestamp e) start) (<= (auditentry-timestamp e) end))) entries))

(defn actor-most-recent-entry [entries actor-id]
  (let [actor-entries (entries-by-actor entries actor-id)]
  (if (empty? actor-entries) nil (let [sorted (reverse (sort-by auditentry-timestamp actor-entries))]
  (first sorted)))))

(defn audit-entry-summary [e]
  (str "Audit #" (auditentry-id e) " | " (auditentry-entity-type e) " #" (auditentry-entity-id e) " | " (auditentry-action e) " | Actor: " (auditentry-actor-id e) " | " (auditentry-detail e)))

(defn audit-entry-detail [e]
  (str "Audit #" (auditentry-id e) " | Entity: " (auditentry-entity-type e) " #" (auditentry-entity-id e) " | Action: " (auditentry-action e) " | Actor: " (auditentry-actor-id e) " | Timestamp: " (auditentry-timestamp e) " | Detail: " (auditentry-detail e)))

(defn valid-audit-entry? [e]
  (and (> (auditentry-id e) 0) (not= (auditentry-entity-type e) "") (> (auditentry-entity-id e) 0) (not= (auditentry-action e) "") (> (auditentry-actor-id e) 0) (> (auditentry-timestamp e) 0)))

(defn audit-entry-count [entries]
  (count entries))

(defn action-distribution [entries]
  (let [creates (count (entries-by-action entries "create"))
   updates (count (entries-by-action entries "update"))
   cancels (count (entries-by-action entries "cancel"))
   deletes (count (entries-by-action entries "delete"))]
  {:create creates :update updates :cancel cancels :delete deletes}))

(defn entity-type-distribution [entries]
  (let [orders-cnt (entry-count-by-type entries "order")
   shipments-cnt (entry-count-by-type entries "shipment")
   invoices-cnt (entry-count-by-type entries "invoice")
   products-cnt (entry-count-by-type entries "product")]
  {:order orders-cnt :shipment shipments-cnt :invoice invoices-cnt :product products-cnt}))

(defn actor-activity-summary [entries]
  (let [actors (distinct-actors entries)]
  (mapv (fn [aid] {:actor-id aid :entry-count (actor-action-count entries aid)}) actors)))

(defn most-active-actor [entries]
  (let [actors (distinct-actors entries)]
  (if (empty? actors) 0 (reduce (fn [best aid] (if (> (actor-action-count entries aid) (actor-action-count entries best)) aid best)) (first actors) (rest actors)))))

(defn entries-per-day [entries start end]
  (let [period-entries (entries-in-period entries start end)
   days (quot (- end start) 86400)]
  (if (<= days 0) (count period-entries) (quot (count period-entries) days))))

(defn reconcile-revenue [orders invoices period-start period-end]
  (let [period-orders (ord/orders-in-period orders period-start period-end)
   expected (ord/total-revenue period-orders)
   period-invoices (filterv (fn [inv] (and (>= (invoice-issued-at inv) period-start) (<= (invoice-issued-at inv) period-end))) invoices)
   actual (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 period-invoices)
   disc (- expected actual)
   status (if (= disc 0) "balanced" "discrepancy")
   period-label (str period-start "-" period-end)]
  (->ReconciliationResult period-label expected actual disc status)))

(defn reconcile-inventory [levels products movements]
  (let [expected (inv/total-inventory-value levels products)
   actual (reduce (fn [acc m] (let [prod (cat/find-product-by-id products (inv/stockmovement-product-id m))
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))
   qty (inv/stockmovement-quantity-change m)]
  (+ acc (* qty cost)))) 0 movements)
   disc (- expected actual)
   status (if (= disc 0) "balanced" "discrepancy")]
  (->ReconciliationResult "current" expected actual disc status)))

(defn reconciliation-healthy? [result]
  (= (reconciliationresult-status result) "balanced"))

(defn reconcile-billing-payments [invoices payments]
  (let [total-billed (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 invoices)
   total-collected (total-revenue-collected payments)
   disc (- total-billed total-collected)
   status (if (= disc 0) "balanced" "discrepancy")]
  (->ReconciliationResult "billing" total-billed total-collected disc status)))

(defn reconcile-shipping-orders [orders shipments]
  (let [active (ord/active-orders orders)
   expected (count active)
   shipped (count (ship/orders-with-shipments shipments active))
   disc (- expected shipped)
   status (if (= disc 0) "balanced" "discrepancy")]
  (->ReconciliationResult "shipping-coverage" (long expected) (long shipped) (long disc) status)))

(defn reconcile-procurement-inventory [pos levels products]
  (let [expected (proc/total-procurement-spend pos)
   actual (inv/total-inventory-value levels products)
   disc (- expected actual)
   status (if (= disc 0) "balanced" "discrepancy")]
  (->ReconciliationResult "procurement-inventory" expected actual disc status)))

(defn reconciliation-result-summary [r]
  (str "Period: " (reconciliationresult-period r) " | Expected: " (reconciliationresult-expected r) " | Actual: " (reconciliationresult-actual r) " | Discrepancy: " (reconciliationresult-discrepancy r) " | Status: " (reconciliationresult-status r)))

(defn discrepancy-pct [result]
  (let [exp (reconciliationresult-expected result)]
  (if (= exp 0) 0 (let [disc (reconciliationresult-discrepancy result)
   abs-disc (if (< disc 0) (- 0 disc) disc)]
  (quot (* abs-disc 100) exp)))))

(defn check-order-has-customer [order customers]
  (let [cust-id (ord/order-customer-id order)
   customer (cust/find-customer-by-id customers cust-id)
   passed (not (nil? customer))
   detail (if passed (str "Customer #" cust-id " exists") (str "Customer #" cust-id " not found"))]
  (->ComplianceCheck 0 "order-has-customer" (ord/order-id order) passed detail 0)))

(defn check-invoice-matches-order [invoice orders]
  (let [order (ord/find-order-by-id orders (invoice-order-id invoice))
   passed (if (nil? order) false (= (invoice-total invoice) (ord/order-total order)))
   detail (if (nil? order) (str "Order #" (invoice-order-id invoice) " not found") (if passed (str "Invoice total matches order total: " (invoice-total invoice)) (str "Invoice total " (invoice-total invoice) " != order total " (ord/order-total order))))]
  (->ComplianceCheck 0 "invoice-matches-order" (invoice-id invoice) passed detail 0)))

(defn check-shipment-has-order [shipment orders]
  (let [order-id (ship/shipment-order-id shipment)
   order (ord/find-order-by-id orders order-id)
   passed (not (nil? order))
   detail (if passed (str "Order #" order-id " exists") (str "Order #" order-id " not found"))]
  (->ComplianceCheck 0 "shipment-has-order" (ship/shipment-id shipment) passed detail 0)))

(defn check-payment-within-invoice [payment invoices]
  (let [inv (find-invoice-by-id invoices (payment-invoice-id payment))
   passed (if (nil? inv) false (<= (payment-amount payment) (invoice-total inv)))
   detail (if (nil? inv) (str "Invoice #" (payment-invoice-id payment) " not found") (if passed (str "Payment " (payment-amount payment) " <= invoice total " (invoice-total inv)) (str "Payment " (payment-amount payment) " > invoice total " (invoice-total inv))))]
  (->ComplianceCheck 0 "payment-within-invoice" (payment-id payment) passed detail 0)))

(defn check-employee-exists [actor-id employees]
  (let [employee (emp/find-employee-by-id employees actor-id)
   passed (not (nil? employee))
   detail (if passed (str "Employee #" actor-id " exists: " (emp/employee-name employee)) (str "Employee #" actor-id " not found"))]
  (->ComplianceCheck 0 "employee-exists" actor-id passed detail 0)))

(defn check-product-active [product]
  (let [passed (cat/product-active product)
   detail (if passed (str "Product #" (cat/product-id product) " is active") (str "Product #" (cat/product-id product) " is inactive"))]
  (->ComplianceCheck 0 "product-active" (cat/product-id product) passed detail 0)))

(defn check-invoice-not-overdue [invoice now]
  (let [is-overdue (and (= (invoice-status invoice) "unpaid") (< (invoice-due-at invoice) now))
   passed (not is-overdue)
   detail (if passed (str "Invoice #" (invoice-id invoice) " is current") (str "Invoice #" (invoice-id invoice) " is overdue"))]
  (->ComplianceCheck 0 "invoice-not-overdue" (invoice-id invoice) passed detail now)))

(defn check-po-within-budget [po budget-limit]
  (let [po-total (proc/purchaseorder-total po)
   passed (<= po-total budget-limit)
   detail (if passed (str "PO #" (proc/purchaseorder-id po) " total " po-total " within budget " budget-limit) (str "PO #" (proc/purchaseorder-id po) " total " po-total " exceeds budget " budget-limit))]
  (->ComplianceCheck 0 "po-within-budget" (proc/purchaseorder-id po) passed detail 0)))

(defn run-order-checks [orders customers]
  (mapv (fn [o] (check-order-has-customer o customers)) orders))

(defn run-invoice-checks [invoices orders]
  (mapv (fn [inv] (check-invoice-matches-order inv orders)) invoices))

(defn run-shipment-checks [shipments orders]
  (mapv (fn [s] (check-shipment-has-order s orders)) shipments))

(defn run-payment-checks [payments invoices]
  (mapv (fn [p] (check-payment-within-invoice p invoices)) payments))

(defn compliance-pass-rate [checks]
  (let [total (count checks)]
  (if (= total 0) 100 (let [passed-count (count (filterv (fn [c] (compliancecheck-passed c)) checks))]
  (quot (* passed-count 100) total)))))

(defn failed-checks [checks]
  (filterv (fn [c] (not (compliancecheck-passed c))) checks))

(defn passed-checks [checks]
  (filterv (fn [c] (compliancecheck-passed c)) checks))

(defn compliance-summary [order-checks invoice-checks shipment-checks]
  (let [all-checks (into [] (concat order-checks invoice-checks shipment-checks))
   total (count all-checks)
   passed-cnt (count (passed-checks all-checks))
   failed-cnt (count (failed-checks all-checks))
   rate (compliance-pass-rate all-checks)]
  {:total-checks total :passed passed-cnt :failed failed-cnt :pass-rate rate}))

(defn compliance-check-summary [c]
  (str "Check #" (compliancecheck-id c) " | " (compliancecheck-check-type c) " | Entity: " (compliancecheck-entity-id c) " | " (if (compliancecheck-passed c) "PASSED" "FAILED") " | " (compliancecheck-detail c)))

(defn audit-order-lifecycle [base-id order actor-id timestamp]
  (let [oid (ord/order-id order)
   status (ord/order-status order)]
  [(->AuditEntry base-id "order" oid "create" actor-id timestamp "pending") (->AuditEntry (+ base-id 1) "order" oid "update" actor-id (+ timestamp 1) status)]))

(defn audit-shipment-lifecycle [base-id shipment actor-id timestamp]
  (let [sid (ship/shipment-id shipment)
   status (ship/shipment-status shipment)]
  [(->AuditEntry base-id "shipment" sid "create" actor-id timestamp "pending") (->AuditEntry (+ base-id 1) "shipment" sid "update" actor-id (+ timestamp 1) status)]))

(defn audit-invoice-lifecycle [base-id invoice actor-id timestamp]
  (let [iid (invoice-id invoice)
   status (invoice-status invoice)]
  [(->AuditEntry base-id "invoice" iid "create" actor-id timestamp "unpaid") (->AuditEntry (+ base-id 1) "invoice" iid "update" actor-id (+ timestamp 1) status)]))

(defn check-order-invoice-coverage [orders invoices]
  (mapv (fn [o] (let [oid (ord/order-id o)
   matching (invoices-for-order invoices oid)
   passed (> (count matching) 0)
   detail (if passed (str "Order #" oid " has invoice") (str "Order #" oid " missing invoice"))]
  (->ComplianceCheck 0 "order-invoice-coverage" oid passed detail 0))) orders))

(defn check-shipment-carrier-valid [shipments carriers]
  (mapv (fn [s] (let [cid (ship/shipment-carrier-id s)
   carrier (ship/find-carrier-by-id carriers cid)
   passed (not (nil? carrier))
   detail (if passed (str "Carrier #" cid " exists") (str "Carrier #" cid " not found"))]
  (->ComplianceCheck 0 "shipment-carrier-valid" (ship/shipment-id s) passed detail 0))) shipments))

(defn check-notification-template-valid [notifications templates]
  (mapv (fn [n] (let [tid (notif/notification-template-id n)
   tmpl (notif/find-template-by-id templates tid)
   passed (not (nil? tmpl))
   detail (if passed (str "Template #" tid " exists") (str "Template #" tid " not found"))]
  (->ComplianceCheck 0 "notification-template-valid" (notif/notification-id n) passed detail 0))) notifications))

(defn total-system-revenue [payments]
  (total-revenue-collected payments))

(defn revenue-vs-shipping [orders shipments]
  (let [rev (ord/total-revenue orders)
   ship-cost (ship/total-shipping-revenue shipments)
   net (- rev ship-cost)
   ship-pct (if (= rev 0) 0 (quot (* ship-cost 100) rev))]
  {:revenue rev :shipping-cost ship-cost :net-after-shipping net :shipping-pct ship-pct}))

(defn revenue-vs-procurement [orders pos]
  (let [rev (ord/total-revenue orders)
   spend (proc/total-procurement-spend pos)
   margin (- rev spend)
   margin-pct (if (= rev 0) 0 (quot (* margin 100) rev))]
  {:revenue rev :procurement-spend spend :gross-margin margin :margin-pct margin-pct}))

(defn revenue-vs-commissions [orders rules employees products]
  (let [rev (ord/total-revenue orders)
   total-comm (reduce (fn [acc e] (+ acc (emp/total-commission rules (emp/employee-id e) orders products))) 0 (emp/active-employees employees))
   comm-pct (if (= rev 0) 0 (quot (* total-comm 100) rev))]
  {:revenue rev :total-commissions total-comm :commission-pct comm-pct}))

(defn inventory-health-check [levels products orders]
  (let [total-value (inv/total-inventory-value levels products)
   low-stock (count (inv/low-stock-items levels))
   out-of-stock (count (inv/out-of-stock-items levels))
   total-items (count levels)
   health-pct (if (= total-items 0) 100 (quot (* (- total-items low-stock) 100) total-items))
   reorder-cost (inv/reorder-cost levels products)]
  {:total-value total-value :low-stock-count low-stock :out-of-stock-count out-of-stock :total-items total-items :health-pct health-pct :reorder-cost reorder-cost}))

(defn customer-health-check [customer orders invoices payments notifications]
  (let [cid (cust/customer-id customer)
   order-count (ord/customer-order-count orders cid)
   total-spend (ord/customer-total-spend orders cid)
   outstanding (customer-outstanding-balance invoices payments cid)
   unpaid-count (customer-unpaid-count invoices cid)
   notif-count (notif/customer-notification-count notifications cid)
   tier (cust/customer-tier customer)]
  {:customer-id cid :name (cust/customer-name customer) :tier tier :order-count order-count :total-spend total-spend :outstanding-balance outstanding :unpaid-invoices unpaid-count :notification-count notif-count}))

(defn promotion-impact-assessment [orders campaigns coupons now]
  (let [total-rev (ord/total-revenue orders)
   total-disc (ord/total-discounts-given orders)
   active-camps (count (promo/active-campaigns campaigns now))
   total-coupon-uses (promo/total-coupon-uses coupons)
   disc-pct (if (= total-rev 0) 0 (quot (* total-disc 100) (+ total-rev total-disc)))]
  {:total-revenue total-rev :total-discounts total-disc :discount-pct disc-pct :active-campaigns active-camps :total-coupon-uses total-coupon-uses}))

(defn employee-audit-trail [entries employee-id employees]
  (let [employee (emp/find-employee-by-id employees employee-id)
   actor-entries (entries-by-actor entries employee-id)
   name (if (nil? employee) "unknown" (emp/employee-name employee))]
  {:employee-id employee-id :employee-name name :entry-count (count actor-entries) :entries actor-entries}))

(defn notification-compliance-check [notifications templates]
  (let [total (count notifications)
   del-rate (notif/delivery-rate-pct notifications)
   sent-cnt (notif/notifications-sent-count notifications)
   failed-cnt (notif/notifications-failed-count notifications)
   avg-time (notif/avg-send-time notifications)]
  {:total-notifications total :delivery-rate del-rate :sent-count sent-cnt :failed-count failed-cnt :avg-send-time avg-time :healthy (>= del-rate 80)}))

(defn audit-dashboard [entries orders invoices payments shipments levels products movements]
  (let [entry-cnt (count entries)
   action-dist (action-distribution entries)
   entity-dist (entity-type-distribution entries)
   rev-recon (reconcile-revenue orders invoices 0 999999999)
   inv-recon (reconcile-inventory levels products movements)
   bill-recon (reconcile-billing-payments invoices payments)]
  {:audit-entries entry-cnt :action-distribution action-dist :entity-distribution entity-dist :revenue-reconciliation {:status (reconciliationresult-status rev-recon) :discrepancy (reconciliationresult-discrepancy rev-recon)} :inventory-reconciliation {:status (reconciliationresult-status inv-recon) :discrepancy (reconciliationresult-discrepancy inv-recon)} :billing-reconciliation {:status (reconciliationresult-status bill-recon) :discrepancy (reconciliationresult-discrepancy bill-recon)}}))

(defn full-compliance-report [orders customers invoices shipments payments]
  (let [order-checks (run-order-checks orders customers)
   invoice-checks (run-invoice-checks invoices orders)
   shipment-checks (run-shipment-checks shipments orders)
   payment-checks (run-payment-checks payments invoices)
   all-checks (into [] (concat order-checks invoice-checks shipment-checks payment-checks))
   total (count all-checks)
   passed-cnt (count (passed-checks all-checks))
   failed-cnt (count (failed-checks all-checks))
   rate (compliance-pass-rate all-checks)]
  {:order-checks {:total (count order-checks) :pass-rate (compliance-pass-rate order-checks)} :invoice-checks {:total (count invoice-checks) :pass-rate (compliance-pass-rate invoice-checks)} :shipment-checks {:total (count shipment-checks) :pass-rate (compliance-pass-rate shipment-checks)} :payment-checks {:total (count payment-checks) :pass-rate (compliance-pass-rate payment-checks)} :overall {:total-checks total :passed passed-cnt :failed failed-cnt :pass-rate rate}}))

(defn system-health-score [orders invoices payments shipments levels products notifications]
  (let [coll-rate (collection-rate-pct invoices payments)
   total-items (count levels)
   low-stock (count (inv/low-stock-items levels))
   inv-health (if (= total-items 0) 100 (quot (* (- total-items low-stock) 100) total-items))
   notif-health (notif/delivery-rate-pct notifications)
   active (ord/active-orders orders)
   active-count (count active)
   shipped-count (count (ship/orders-with-shipments shipments active))
   ship-health (if (= active-count 0) 100 (quot (* shipped-count 100) active-count))
   score (quot (+ (* coll-rate 30) (* inv-health 25) (* notif-health 20) (* ship-health 25)) 100)]
  score))

(defn sort-entries-by-timestamp [entries]
  (sort-by auditentry-timestamp entries))

(defn sort-entries-by-id [entries]
  (sort-by auditentry-id entries))

(defn entries-with-detail [entries detail]
  (filterv (fn [e] (= (auditentry-detail e) detail)) entries))

(defn entries-for-entity-action [entries entity-type action]
  (filterv (fn [e] (and (= (auditentry-entity-type e) entity-type) (= (auditentry-action e) action))) entries))

(defn audit-kpi-snapshot [orders invoices payments shipments levels products]
  (let [kpi (analytics/kpi-dashboard orders invoices payments shipments levels products)]
  {:total-revenue (:total-revenue kpi) :total-collected (:total-collected kpi) :total-outstanding (:total-outstanding kpi) :total-shipping (:total-shipping kpi) :inventory-value (:inventory-value kpi) :order-count (:order-count kpi)}))

(defn high-value-order-entries [entries orders threshold]
  (let [high-orders (ord/orders-above-total orders threshold)
   high-ids (mapv (fn [o] (ord/order-id o)) high-orders)]
  (filterv (fn [e] (and (= (auditentry-entity-type e) "order") (some (fn [oid] (= (auditentry-entity-id e) oid)) high-ids))) entries)))

(defn actors-with-cancellations [entries]
  (let [cancel-entries (entries-by-action entries "cancel")]
  (distinct (mapv auditentry-actor-id cancel-entries))))

(defn entity-change-frequency [entries entity-type entity-id]
  (count (entries-for-entity entries entity-type entity-id)))

(defn entities-with-high-churn [entries entity-type threshold]
  (let [type-entries (entries-by-entity-type entries entity-type)
   entity-ids (distinct (mapv auditentry-entity-id type-entries))]
  (filterv (fn [eid] (> (entity-change-frequency entries entity-type eid) threshold)) entity-ids)))

(defn audit-period-report [entries start end]
  (let [period-entries (entries-in-period entries start end)
   total (count period-entries)
   actors (distinct-actors period-entries)
   creates (count (entries-by-action period-entries "create"))
   updates (count (entries-by-action period-entries "update"))
   cancels (count (entries-by-action period-entries "cancel"))]
  {:period-start start :period-end end :total-entries total :unique-actors (count actors) :creates creates :updates updates :cancels cancels}))

(defn orphan-invoices [invoices orders]
  (filterv (fn [inv] (nil? (ord/find-order-by-id orders (invoice-order-id inv)))) invoices))

(defn orphan-shipments [shipments orders]
  (filterv (fn [s] (nil? (ord/find-order-by-id orders (ship/shipment-order-id s)))) shipments))

(defn orphan-payments [payments invoices]
  (filterv (fn [p] (nil? (find-invoice-by-id invoices (payment-invoice-id p)))) payments))

(defn data-integrity-report [orders invoices payments shipments]
  (let [orphan-inv (orphan-invoices invoices orders)
   orphan-ship (orphan-shipments shipments orders)
   orphan-pay (orphan-payments payments invoices)
   clean (and (= (count orphan-inv) 0) (= (count orphan-ship) 0) (= (count orphan-pay) 0))]
  {:orphan-invoices (count orphan-inv) :orphan-shipments (count orphan-ship) :orphan-payments (count orphan-pay) :data-clean clean}))
