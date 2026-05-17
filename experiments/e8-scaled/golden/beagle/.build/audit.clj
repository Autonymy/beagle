(ns audit
  (:require [catalog :as cat]
            [customers :as cust]
            [inventory :as inv]
            [orders :as ord]
            [reports :as rep]
            [shipping :as ship]
            [billing :as billing]
            [procurement :as proc]
            [promotions :as promo]
            [employees :as emp]
            [analytics :as analytics]
            [notifications :as notif]))

^{:line 23 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defrecord AuditEntry [id entity-type entity-id action actor-id timestamp detail])

(defn auditentry-id [r] (:id r))

(defn auditentry-entity-type [r] (:entity-type r))

(defn auditentry-entity-id [r] (:entity-id r))

(defn auditentry-action [r] (:action r))

(defn auditentry-actor-id [r] (:actor-id r))

(defn auditentry-timestamp [r] (:timestamp r))

(defn auditentry-detail [r] (:detail r))

^{:line 27 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defrecord ReconciliationResult [period expected actual discrepancy status])

(defn reconciliationresult-period [r] (:period r))

(defn reconciliationresult-expected [r] (:expected r))

(defn reconciliationresult-actual [r] (:actual r))

(defn reconciliationresult-discrepancy [r] (:discrepancy r))

(defn reconciliationresult-status [r] (:status r))

^{:line 31 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defrecord ComplianceCheck [id check-type entity-id passed detail checked-at])

(defn compliancecheck-id [r] (:id r))

(defn compliancecheck-check-type [r] (:check-type r))

(defn compliancecheck-entity-id [r] (:entity-id r))

(defn compliancecheck-passed [r] (:passed r))

(defn compliancecheck-detail [r] (:detail r))

(defn compliancecheck-checked-at [r] (:checked-at r))

^{:line 40 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn create-audit-entry [id entity-type entity-id action actor-id timestamp detail]
  ^{:line 43 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry id entity-type entity-id action actor-id timestamp detail))

^{:line 47 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-order [id order actor-id action timestamp]
  ^{:line 49 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry id "order" ^{:line 49 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-id order) action actor-id timestamp ^{:line 50 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-status order)))

^{:line 54 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-shipment [id shipment actor-id action timestamp]
  ^{:line 56 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry id "shipment" ^{:line 56 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-id shipment) action actor-id timestamp ^{:line 57 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-status shipment)))

^{:line 61 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-invoice [id invoice actor-id action timestamp]
  ^{:line 63 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry id "invoice" ^{:line 63 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice) action actor-id timestamp ^{:line 64 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-status invoice)))

^{:line 68 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-product [id product actor-id action timestamp]
  ^{:line 70 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry id "product" ^{:line 70 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-id product) action actor-id timestamp ^{:line 71 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-name product)))

^{:line 78 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-for-entity [entries entity-type entity-id]
  ^{:line 80 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 80 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 80 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 80 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 80 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) entity-type) ^{:line 81 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 81 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-id e) entity-id))) entries))

^{:line 85 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-by-actor [entries actor-id]
  ^{:line 86 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 86 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 86 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 86 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-actor-id e) actor-id)) entries))

^{:line 89 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-by-action [entries action]
  ^{:line 90 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 90 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 90 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 90 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-action e) action)) entries))

^{:line 93 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-in-period [entries start end]
  ^{:line 94 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 94 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 94 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 94 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (>= ^{:line 94 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) start) ^{:line 95 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= ^{:line 95 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) end))) entries))

^{:line 99 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entry-count-by-type [entries entity-type]
  ^{:line 100 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 100 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 100 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 100 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 100 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) entity-type)) entries)))

^{:line 104 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn recent-entries [entries n]
  ^{:line 105 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [sorted ^{:line 105 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reverse ^{:line 105 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (sort-by auditentry-timestamp entries))]
  ^{:line 106 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (vec ^{:line 106 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (take n sorted))))

^{:line 109 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn actor-action-count [entries actor-id]
  ^{:line 110 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 110 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-actor entries actor-id)))

^{:line 117 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-by-entity-type [entries entity-type]
  ^{:line 118 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 118 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 118 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 118 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) entity-type)) entries))

^{:line 121 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn distinct-actors [entries]
  ^{:line 122 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct ^{:line 122 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv auditentry-actor-id entries)))

^{:line 125 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn distinct-entity-types [entries]
  ^{:line 126 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct ^{:line 126 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv auditentry-entity-type entries)))

^{:line 129 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-for-actor-in-period [entries actor-id start end]
  ^{:line 131 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 131 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 131 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 131 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 131 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-actor-id e) actor-id) ^{:line 132 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (>= ^{:line 132 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) start) ^{:line 133 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= ^{:line 133 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) end))) entries))

^{:line 137 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn actor-most-recent-entry [entries actor-id]
  ^{:line 138 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [actor-entries ^{:line 138 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-actor entries actor-id)]
  ^{:line 139 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 139 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (empty? actor-entries) nil ^{:line 141 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [sorted ^{:line 141 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reverse ^{:line 141 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (sort-by auditentry-timestamp actor-entries))]
  ^{:line 142 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (first sorted)))))

^{:line 149 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-entry-summary [e]
  ^{:line 150 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Audit #" ^{:line 150 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-id e) " | " ^{:line 151 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) " #" ^{:line 151 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-id e) " | " ^{:line 152 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-action e) " | Actor: " ^{:line 153 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-actor-id e) " | " ^{:line 154 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-detail e)))

^{:line 157 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-entry-detail [e]
  ^{:line 158 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Audit #" ^{:line 158 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-id e) " | Entity: " ^{:line 159 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) " #" ^{:line 159 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-id e) " | Action: " ^{:line 160 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-action e) " | Actor: " ^{:line 161 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-actor-id e) " | Timestamp: " ^{:line 162 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) " | Detail: " ^{:line 163 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-detail e)))

^{:line 170 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn valid-audit-entry? [e]
  ^{:line 171 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 171 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 171 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-id e) 0) ^{:line 172 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not= ^{:line 172 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) "") ^{:line 173 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 173 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-id e) 0) ^{:line 174 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not= ^{:line 174 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-action e) "") ^{:line 175 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 175 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-actor-id e) 0) ^{:line 176 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 176 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-timestamp e) 0)))

^{:line 183 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-entry-count [entries]
  ^{:line 184 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count entries))

^{:line 187 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn action-distribution [entries]
  ^{:line 188 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [creates ^{:line 188 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 188 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action entries "create"))
   updates ^{:line 189 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 189 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action entries "update"))
   cancels ^{:line 190 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 190 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action entries "cancel"))
   deletes ^{:line 191 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 191 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action entries "delete"))]
  ^{:line 192 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:create creates :update updates :cancel cancels :delete deletes}))

^{:line 195 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entity-type-distribution [entries]
  ^{:line 196 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [orders-cnt ^{:line 196 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entry-count-by-type entries "order")
   shipments-cnt ^{:line 197 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entry-count-by-type entries "shipment")
   invoices-cnt ^{:line 198 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entry-count-by-type entries "invoice")
   products-cnt ^{:line 199 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entry-count-by-type entries "product")]
  ^{:line 200 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:order orders-cnt :shipment shipments-cnt :invoice invoices-cnt :product products-cnt}))

^{:line 204 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn actor-activity-summary [entries]
  ^{:line 205 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [actors ^{:line 205 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct-actors entries)]
  ^{:line 206 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 206 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [aid] ^{:line 207 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:actor-id aid :entry-count ^{:line 207 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (actor-action-count entries aid)}) actors)))

^{:line 212 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn most-active-actor [entries]
  ^{:line 213 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [actors ^{:line 213 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct-actors entries)]
  ^{:line 214 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 214 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (empty? actors) 0 ^{:line 216 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reduce ^{:line 216 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [best aid] ^{:line 217 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 217 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 217 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (actor-action-count entries aid) ^{:line 218 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (actor-action-count entries best)) aid best)) ^{:line 221 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (first actors) ^{:line 222 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (rest actors)))))

^{:line 225 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-per-day [entries start end]
  ^{:line 226 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [period-entries ^{:line 226 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-in-period entries start end)
   days ^{:line 227 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 227 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- end start) 86400)]
  ^{:line 228 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 228 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= days 0) ^{:line 228 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count period-entries) ^{:line 229 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 229 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count period-entries) days))))

^{:line 242 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconcile-revenue [orders invoices period-start period-end]
  ^{:line 244 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [period-orders ^{:line 244 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/orders-in-period orders period-start period-end)
   expected ^{:line 245 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-revenue period-orders)
   period-invoices ^{:line 246 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 246 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [inv] ^{:line 247 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 247 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (>= ^{:line 247 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-issued-at inv) period-start) ^{:line 248 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= ^{:line 248 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-issued-at inv) period-end))) invoices)
   actual ^{:line 250 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reduce ^{:line 250 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [acc inv] ^{:line 250 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ acc ^{:line 250 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total inv))) 0 period-invoices)
   disc ^{:line 252 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- expected actual)
   status ^{:line 253 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 253 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= disc 0) "balanced" "discrepancy")
   period-label ^{:line 254 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str period-start "-" period-end)]
  ^{:line 255 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ReconciliationResult period-label expected actual disc status)))

^{:line 263 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconcile-inventory [levels products movements]
  ^{:line 265 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [expected ^{:line 265 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/total-inventory-value levels products)
   actual ^{:line 266 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reduce ^{:line 266 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [acc m] ^{:line 267 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [prod ^{:line 267 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/find-product-by-id products ^{:line 269 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/stockmovement-product-id m))
   cost ^{:line 270 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 270 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? prod) 0 ^{:line 270 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-unit-cost prod))
   qty ^{:line 271 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/stockmovement-quantity-change m)]
  ^{:line 272 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ acc ^{:line 272 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* qty cost)))) 0 movements)
   disc ^{:line 274 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- expected actual)
   status ^{:line 275 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 275 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= disc 0) "balanced" "discrepancy")]
  ^{:line 276 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ReconciliationResult "current" expected actual disc status)))

^{:line 279 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconciliation-healthy? [result]
  ^{:line 280 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 280 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-status result) "balanced"))

^{:line 287 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconcile-billing-payments [invoices payments]
  ^{:line 289 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [total-billed ^{:line 289 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reduce ^{:line 289 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [acc inv] ^{:line 289 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ acc ^{:line 289 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total inv))) 0 invoices)
   total-collected ^{:line 291 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/total-revenue-collected payments)
   disc ^{:line 292 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- total-billed total-collected)
   status ^{:line 293 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 293 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= disc 0) "balanced" "discrepancy")]
  ^{:line 294 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ReconciliationResult "billing" total-billed total-collected disc status)))

^{:line 299 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconcile-shipping-orders [orders shipments]
  ^{:line 301 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [active ^{:line 301 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/active-orders orders)
   expected ^{:line 302 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count active)
   shipped ^{:line 303 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 303 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/orders-with-shipments shipments active))
   disc ^{:line 304 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- expected shipped)
   status ^{:line 305 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 305 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= disc 0) "balanced" "discrepancy")]
  ^{:line 306 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ReconciliationResult "shipping-coverage" ^{:line 307 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (long expected) ^{:line 308 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (long shipped) ^{:line 309 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (long disc) status)))

^{:line 315 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconcile-procurement-inventory [pos levels products]
  ^{:line 317 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [expected ^{:line 317 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/total-procurement-spend pos)
   actual ^{:line 318 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/total-inventory-value levels products)
   disc ^{:line 319 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- expected actual)
   status ^{:line 320 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 320 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= disc 0) "balanced" "discrepancy")]
  ^{:line 321 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ReconciliationResult "procurement-inventory" expected actual disc status)))

^{:line 324 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn reconciliation-result-summary [r]
  ^{:line 325 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Period: " ^{:line 325 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-period r) " | Expected: " ^{:line 326 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-expected r) " | Actual: " ^{:line 327 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-actual r) " | Discrepancy: " ^{:line 328 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-discrepancy r) " | Status: " ^{:line 329 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-status r)))

^{:line 332 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn discrepancy-pct [result]
  ^{:line 333 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [exp ^{:line 333 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-expected result)]
  ^{:line 334 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 334 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= exp 0) 0 ^{:line 335 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [disc ^{:line 335 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-discrepancy result)
   abs-disc ^{:line 336 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 336 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (< disc 0) ^{:line 336 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- 0 disc) disc)]
  ^{:line 337 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 337 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* abs-disc 100) exp)))))

^{:line 345 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-order-has-customer [order customers]
  ^{:line 346 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [cust-id ^{:line 346 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-customer-id order)
   customer ^{:line 347 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cust/find-customer-by-id customers cust-id)
   passed ^{:line 348 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 348 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? customer))
   detail ^{:line 349 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 350 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Customer #" cust-id " exists") ^{:line 351 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Customer #" cust-id " not found"))]
  ^{:line 352 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "order-has-customer" ^{:line 352 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-id order) passed detail 0)))

^{:line 357 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-invoice-matches-order [invoice orders]
  ^{:line 359 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [order ^{:line 359 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/find-order-by-id orders ^{:line 359 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-order-id invoice))
   passed ^{:line 360 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 360 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? order) false ^{:line 362 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 362 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total invoice) ^{:line 362 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-total order)))
   detail ^{:line 363 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 363 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? order) ^{:line 364 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Order #" ^{:line 364 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-order-id invoice) " not found") ^{:line 365 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 366 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Invoice total matches order total: " ^{:line 367 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total invoice)) ^{:line 368 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Invoice total " ^{:line 368 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total invoice) " != order total " ^{:line 369 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-total order))))]
  ^{:line 370 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "invoice-matches-order" ^{:line 370 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice) passed detail 0)))

^{:line 374 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-shipment-has-order [shipment orders]
  ^{:line 376 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [order-id ^{:line 376 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-order-id shipment)
   order ^{:line 377 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/find-order-by-id orders order-id)
   passed ^{:line 378 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 378 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? order))
   detail ^{:line 379 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 380 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Order #" order-id " exists") ^{:line 381 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Order #" order-id " not found"))]
  ^{:line 382 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "shipment-has-order" ^{:line 382 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-id shipment) passed detail 0)))

^{:line 386 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-payment-within-invoice [payment invoices]
  ^{:line 388 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [inv ^{:line 388 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/find-invoice-by-id invoices ^{:line 388 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-invoice-id payment))
   passed ^{:line 389 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 389 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? inv) false ^{:line 391 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= ^{:line 391 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-amount payment) ^{:line 391 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total inv)))
   detail ^{:line 392 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 392 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? inv) ^{:line 393 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Invoice #" ^{:line 393 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-invoice-id payment) " not found") ^{:line 394 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 395 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Payment " ^{:line 395 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-amount payment) " <= invoice total " ^{:line 396 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total inv)) ^{:line 397 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Payment " ^{:line 397 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-amount payment) " > invoice total " ^{:line 398 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-total inv))))]
  ^{:line 399 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "payment-within-invoice" ^{:line 399 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-id payment) passed detail 0)))

^{:line 407 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-employee-exists [actor-id employees]
  ^{:line 409 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [employee ^{:line 409 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/find-employee-by-id employees actor-id)
   passed ^{:line 410 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 410 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? employee))
   detail ^{:line 411 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 412 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Employee #" actor-id " exists: " ^{:line 413 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/employee-name employee)) ^{:line 414 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Employee #" actor-id " not found"))]
  ^{:line 415 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "employee-exists" actor-id passed detail 0)))

^{:line 418 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-product-active [product]
  ^{:line 419 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [passed ^{:line 419 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-active product)
   detail ^{:line 420 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 421 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Product #" ^{:line 421 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-id product) " is active") ^{:line 422 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Product #" ^{:line 422 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-id product) " is inactive"))]
  ^{:line 423 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "product-active" ^{:line 423 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cat/product-id product) passed detail 0)))

^{:line 427 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-invoice-not-overdue [invoice now]
  ^{:line 429 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [is-overdue ^{:line 429 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 429 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 429 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-status invoice) "unpaid") ^{:line 430 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (< ^{:line 430 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-due-at invoice) now))
   passed ^{:line 431 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not is-overdue)
   detail ^{:line 432 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 433 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Invoice #" ^{:line 433 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice) " is current") ^{:line 434 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Invoice #" ^{:line 434 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice) " is overdue"))]
  ^{:line 435 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "invoice-not-overdue" ^{:line 435 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice) passed detail now)))

^{:line 439 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-po-within-budget [po budget-limit]
  ^{:line 441 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [po-total ^{:line 441 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/purchaseorder-total po)
   passed ^{:line 442 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (<= po-total budget-limit)
   detail ^{:line 443 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 444 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "PO #" ^{:line 444 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/purchaseorder-id po) " total " po-total " within budget " budget-limit) ^{:line 446 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "PO #" ^{:line 446 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/purchaseorder-id po) " total " po-total " exceeds budget " budget-limit))]
  ^{:line 448 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "po-within-budget" ^{:line 448 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/purchaseorder-id po) passed detail 0)))

^{:line 456 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn run-order-checks [orders customers]
  ^{:line 457 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 457 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [o] ^{:line 457 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (check-order-has-customer o customers)) orders))

^{:line 460 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn run-invoice-checks [invoices orders]
  ^{:line 461 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 461 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [inv] ^{:line 461 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (check-invoice-matches-order inv orders)) invoices))

^{:line 464 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn run-shipment-checks [shipments orders]
  ^{:line 465 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 465 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [s] ^{:line 465 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (check-shipment-has-order s orders)) shipments))

^{:line 468 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn run-payment-checks [payments invoices]
  ^{:line 469 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 469 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [p] ^{:line 469 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (check-payment-within-invoice p invoices)) payments))

^{:line 472 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn compliance-pass-rate [checks]
  ^{:line 473 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [total ^{:line 473 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count checks)]
  ^{:line 474 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 474 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= total 0) 100 ^{:line 475 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [passed-count ^{:line 475 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 475 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 475 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [c] ^{:line 476 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-passed c)) checks))]
  ^{:line 478 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 478 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* passed-count 100) total)))))

^{:line 481 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn failed-checks [checks]
  ^{:line 482 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 482 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [c] ^{:line 482 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 482 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-passed c))) checks))

^{:line 486 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn passed-checks [checks]
  ^{:line 487 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 487 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [c] ^{:line 487 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-passed c)) checks))

^{:line 492 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn compliance-summary [order-checks invoice-checks shipment-checks]
  ^{:line 494 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [all-checks ^{:line 494 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (into ^{:line 494 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} [] ^{:line 494 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (concat order-checks invoice-checks shipment-checks))
   total ^{:line 495 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count all-checks)
   passed-cnt ^{:line 496 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 496 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (passed-checks all-checks))
   failed-cnt ^{:line 497 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 497 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (failed-checks all-checks))
   rate ^{:line 498 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate all-checks)]
  ^{:line 499 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-checks total :passed passed-cnt :failed failed-cnt :pass-rate rate}))

^{:line 509 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn compliance-check-summary [c]
  ^{:line 510 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Check #" ^{:line 510 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-id c) " | " ^{:line 511 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-check-type c) " | Entity: " ^{:line 512 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-entity-id c) " | " ^{:line 513 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 513 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-passed c) "PASSED" "FAILED") " | " ^{:line 514 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliancecheck-detail c)))

^{:line 521 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-order-lifecycle [base-id order actor-id timestamp]
  ^{:line 523 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [oid ^{:line 523 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-id order)
   status ^{:line 524 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-status order)]
  ^{:line 525 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} [^{:line 525 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry base-id "order" oid "create" actor-id timestamp "pending") ^{:line 526 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry ^{:line 526 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ base-id 1) "order" oid "update" actor-id ^{:line 527 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ timestamp 1) status)]))

^{:line 530 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-shipment-lifecycle [base-id shipment actor-id timestamp]
  ^{:line 532 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [sid ^{:line 532 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-id shipment)
   status ^{:line 533 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-status shipment)]
  ^{:line 534 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} [^{:line 534 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry base-id "shipment" sid "create" actor-id timestamp "pending") ^{:line 535 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry ^{:line 535 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ base-id 1) "shipment" sid "update" actor-id ^{:line 536 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ timestamp 1) status)]))

^{:line 539 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-invoice-lifecycle [base-id invoice actor-id timestamp]
  ^{:line 541 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [iid ^{:line 541 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-id invoice)
   status ^{:line 542 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-status invoice)]
  ^{:line 543 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} [^{:line 543 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry base-id "invoice" iid "create" actor-id timestamp "unpaid") ^{:line 544 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->AuditEntry ^{:line 544 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ base-id 1) "invoice" iid "update" actor-id ^{:line 545 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ timestamp 1) status)]))

^{:line 552 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-order-invoice-coverage [orders invoices]
  ^{:line 554 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 554 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [o] ^{:line 555 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [oid ^{:line 555 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-id o)
   matching ^{:line 556 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoices-for-order invoices oid)
   passed ^{:line 557 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 557 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count matching) 0)
   detail ^{:line 558 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 559 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Order #" oid " has invoice") ^{:line 560 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Order #" oid " missing invoice"))]
  ^{:line 561 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "order-invoice-coverage" oid passed detail 0))) orders))

^{:line 566 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-shipment-carrier-valid [shipments carriers]
  ^{:line 568 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 568 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [s] ^{:line 569 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [cid ^{:line 569 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-carrier-id s)
   carrier ^{:line 570 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/find-carrier-by-id carriers cid)
   passed ^{:line 571 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 571 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? carrier))
   detail ^{:line 572 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 573 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Carrier #" cid " exists") ^{:line 574 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Carrier #" cid " not found"))]
  ^{:line 575 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "shipment-carrier-valid" ^{:line 576 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-id s) passed detail 0))) shipments))

^{:line 581 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn check-notification-template-valid [notifications templates]
  ^{:line 583 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 583 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [n] ^{:line 584 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [tid ^{:line 584 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/notification-template-id n)
   tmpl ^{:line 585 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/find-template-by-id templates tid)
   passed ^{:line 586 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (not ^{:line 586 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? tmpl))
   detail ^{:line 587 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if passed ^{:line 588 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Template #" tid " exists") ^{:line 589 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (str "Template #" tid " not found"))]
  ^{:line 590 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (->ComplianceCheck 0 "notification-template-valid" ^{:line 591 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/notification-id n) passed detail 0))) notifications))

^{:line 600 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn total-system-revenue [payments]
  ^{:line 601 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/total-revenue-collected payments))

^{:line 604 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn revenue-vs-shipping [orders shipments]
  ^{:line 605 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [rev ^{:line 605 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-revenue orders)
   ship-cost ^{:line 606 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/total-shipping-revenue shipments)
   net ^{:line 607 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- rev ship-cost)
   ship-pct ^{:line 608 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 608 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= rev 0) 0 ^{:line 608 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 608 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* ship-cost 100) rev))]
  ^{:line 609 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:revenue rev :shipping-cost ship-cost :net-after-shipping net :shipping-pct ship-pct}))

^{:line 615 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn revenue-vs-procurement [orders pos]
  ^{:line 616 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [rev ^{:line 616 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-revenue orders)
   spend ^{:line 617 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (proc/total-procurement-spend pos)
   margin ^{:line 618 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- rev spend)
   margin-pct ^{:line 619 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 619 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= rev 0) 0 ^{:line 619 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 619 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* margin 100) rev))]
  ^{:line 620 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:revenue rev :procurement-spend spend :gross-margin margin :margin-pct margin-pct}))

^{:line 626 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn revenue-vs-commissions [orders rules employees products]
  ^{:line 628 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [rev ^{:line 628 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-revenue orders)
   total-comm ^{:line 629 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reduce ^{:line 630 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [acc e] ^{:line 631 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ acc ^{:line 631 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/total-commission rules ^{:line 632 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/employee-id e) orders products))) 0 ^{:line 634 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/active-employees employees))
   comm-pct ^{:line 635 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 635 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= rev 0) 0 ^{:line 635 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 635 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* total-comm 100) rev))]
  ^{:line 636 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:revenue rev :total-commissions total-comm :commission-pct comm-pct}))

^{:line 645 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn inventory-health-check [levels products orders]
  ^{:line 647 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [total-value ^{:line 647 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/total-inventory-value levels products)
   low-stock ^{:line 648 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 648 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/low-stock-items levels))
   out-of-stock ^{:line 649 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 649 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/out-of-stock-items levels))
   total-items ^{:line 650 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count levels)
   health-pct ^{:line 651 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 651 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= total-items 0) 100 ^{:line 652 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 652 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* ^{:line 652 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- total-items low-stock) 100) total-items))
   reorder-cost ^{:line 653 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/reorder-cost levels products)]
  ^{:line 654 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-value total-value :low-stock-count low-stock :out-of-stock-count out-of-stock :total-items total-items :health-pct health-pct :reorder-cost reorder-cost}))

^{:line 666 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn customer-health-check [customer orders invoices payments notifications]
  ^{:line 669 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [cid ^{:line 669 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cust/customer-id customer)
   order-count ^{:line 670 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/customer-order-count orders cid)
   total-spend ^{:line 671 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/customer-total-spend orders cid)
   outstanding ^{:line 672 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/customer-outstanding-balance invoices payments cid)
   unpaid-count ^{:line 673 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/customer-unpaid-count invoices cid)
   notif-count ^{:line 674 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/customer-notification-count notifications cid)
   tier ^{:line 675 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cust/customer-tier customer)]
  ^{:line 676 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:customer-id cid :name ^{:line 676 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (cust/customer-name customer) :tier tier :order-count order-count :total-spend total-spend :outstanding-balance outstanding :unpaid-invoices unpaid-count :notification-count notif-count}))

^{:line 690 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn promotion-impact-assessment [orders campaigns coupons now]
  ^{:line 692 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [total-rev ^{:line 692 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-revenue orders)
   total-disc ^{:line 693 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/total-discounts-given orders)
   active-camps ^{:line 694 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 694 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (promo/active-campaigns campaigns now))
   total-coupon-uses ^{:line 695 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (promo/total-coupon-uses coupons)
   disc-pct ^{:line 696 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 696 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= total-rev 0) 0 ^{:line 697 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 697 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* total-disc 100) ^{:line 697 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ total-rev total-disc)))]
  ^{:line 698 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-revenue total-rev :total-discounts total-disc :discount-pct disc-pct :active-campaigns active-camps :total-coupon-uses total-coupon-uses}))

^{:line 709 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn employee-audit-trail [entries employee-id employees]
  ^{:line 711 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [employee ^{:line 711 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/find-employee-by-id employees employee-id)
   actor-entries ^{:line 712 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-actor entries employee-id)
   name ^{:line 713 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 713 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? employee) "unknown" ^{:line 713 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (emp/employee-name employee))]
  ^{:line 714 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:employee-id employee-id :employee-name name :entry-count ^{:line 714 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count actor-entries) :entries actor-entries}))

^{:line 724 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn notification-compliance-check [notifications templates]
  ^{:line 726 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [total ^{:line 726 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count notifications)
   del-rate ^{:line 727 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/delivery-rate-pct notifications)
   sent-cnt ^{:line 728 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/notifications-sent-count notifications)
   failed-cnt ^{:line 729 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/notifications-failed-count notifications)
   avg-time ^{:line 730 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/avg-send-time notifications)]
  ^{:line 731 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-notifications total :delivery-rate del-rate :sent-count sent-cnt :failed-count failed-cnt :avg-send-time avg-time :healthy ^{:line 731 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (>= del-rate 80)}))

^{:line 743 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-dashboard [entries orders invoices payments shipments levels products movements]
  ^{:line 746 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [entry-cnt ^{:line 747 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count entries)
   action-dist ^{:line 748 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (action-distribution entries)
   entity-dist ^{:line 749 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entity-type-distribution entries)
   rev-recon ^{:line 751 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconcile-revenue orders invoices 0 999999999)
   inv-recon ^{:line 753 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconcile-inventory levels products movements)
   bill-recon ^{:line 755 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconcile-billing-payments invoices payments)]
  ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:audit-entries entry-cnt :action-distribution action-dist :entity-distribution entity-dist :revenue-reconciliation ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:status ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-status rev-recon) :discrepancy ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-discrepancy rev-recon)} :inventory-reconciliation ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:status ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-status inv-recon) :discrepancy ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-discrepancy inv-recon)} :billing-reconciliation ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:status ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-status bill-recon) :discrepancy ^{:line 756 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (reconciliationresult-discrepancy bill-recon)}}))

^{:line 774 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn full-compliance-report [orders customers invoices shipments payments]
  ^{:line 777 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [order-checks ^{:line 777 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (run-order-checks orders customers)
   invoice-checks ^{:line 778 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (run-invoice-checks invoices orders)
   shipment-checks ^{:line 779 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (run-shipment-checks shipments orders)
   payment-checks ^{:line 780 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (run-payment-checks payments invoices)
   all-checks ^{:line 781 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (into ^{:line 781 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} [] ^{:line 781 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (concat order-checks invoice-checks shipment-checks payment-checks))
   total ^{:line 783 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count all-checks)
   passed-cnt ^{:line 784 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 784 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (passed-checks all-checks))
   failed-cnt ^{:line 785 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 785 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (failed-checks all-checks))
   rate ^{:line 786 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate all-checks)]
  ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:order-checks ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count order-checks) :pass-rate ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate order-checks)} :invoice-checks ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count invoice-checks) :pass-rate ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate invoice-checks)} :shipment-checks ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count shipment-checks) :pass-rate ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate shipment-checks)} :payment-checks ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count payment-checks) :pass-rate ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (compliance-pass-rate payment-checks)} :overall ^{:line 787 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-checks total :passed passed-cnt :failed failed-cnt :pass-rate rate}}))

^{:line 806 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn system-health-score [orders invoices payments shipments levels products notifications]
  ^{:line 809 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [coll-rate ^{:line 810 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/collection-rate-pct invoices payments)
   total-items ^{:line 812 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count levels)
   low-stock ^{:line 813 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 813 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (inv/low-stock-items levels))
   inv-health ^{:line 814 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 814 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= total-items 0) 100 ^{:line 815 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 815 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* ^{:line 815 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (- total-items low-stock) 100) total-items))
   notif-health ^{:line 817 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (notif/delivery-rate-pct notifications)
   active ^{:line 819 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/active-orders orders)
   active-count ^{:line 820 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count active)
   shipped-count ^{:line 821 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 821 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/orders-with-shipments shipments active))
   ship-health ^{:line 822 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (if ^{:line 822 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= active-count 0) 100 ^{:line 823 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 823 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* shipped-count 100) active-count))
   score ^{:line 826 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (quot ^{:line 826 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (+ ^{:line 826 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* coll-rate 30) ^{:line 826 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* inv-health 25) ^{:line 827 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* notif-health 20) ^{:line 827 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (* ship-health 25)) 100)]
  score))

^{:line 836 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn sort-entries-by-timestamp [entries]
  ^{:line 837 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (sort-by auditentry-timestamp entries))

^{:line 840 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn sort-entries-by-id [entries]
  ^{:line 841 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (sort-by auditentry-id entries))

^{:line 849 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-with-detail [entries detail]
  ^{:line 850 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 850 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 850 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 850 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-detail e) detail)) entries))

^{:line 853 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entries-for-entity-action [entries entity-type action]
  ^{:line 855 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 855 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 855 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 855 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 855 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) entity-type) ^{:line 856 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 856 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-action e) action))) entries))

^{:line 864 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-kpi-snapshot [orders invoices payments shipments levels products]
  ^{:line 867 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [kpi ^{:line 867 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (analytics/kpi-dashboard orders invoices payments shipments levels products)]
  ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:total-revenue ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:total-revenue kpi) :total-collected ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:total-collected kpi) :total-outstanding ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:total-outstanding kpi) :total-shipping ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:total-shipping kpi) :inventory-value ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:inventory-value kpi) :order-count ^{:line 869 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (:order-count kpi)}))

^{:line 882 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn high-value-order-entries [entries orders threshold]
  ^{:line 884 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [high-orders ^{:line 884 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/orders-above-total orders threshold)
   high-ids ^{:line 885 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv ^{:line 885 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [o] ^{:line 885 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/order-id o)) high-orders)]
  ^{:line 886 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 886 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [e] ^{:line 887 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 887 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 887 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-type e) "order") ^{:line 888 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (some ^{:line 888 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [oid] ^{:line 888 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 888 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (auditentry-entity-id e) oid)) high-ids))) entries)))

^{:line 893 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn actors-with-cancellations [entries]
  ^{:line 894 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [cancel-entries ^{:line 894 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action entries "cancel")]
  ^{:line 895 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct ^{:line 895 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv auditentry-actor-id cancel-entries))))

^{:line 898 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entity-change-frequency [entries entity-type entity-id]
  ^{:line 900 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 900 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-for-entity entries entity-type entity-id)))

^{:line 903 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn entities-with-high-churn [entries entity-type threshold]
  ^{:line 905 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [type-entries ^{:line 905 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-entity-type entries entity-type)
   entity-ids ^{:line 906 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct ^{:line 906 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (mapv auditentry-entity-id type-entries))]
  ^{:line 907 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 907 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [eid] ^{:line 908 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (> ^{:line 908 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entity-change-frequency entries entity-type eid) threshold)) entity-ids)))

^{:line 916 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn audit-period-report [entries start end]
  ^{:line 917 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [period-entries ^{:line 917 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-in-period entries start end)
   total ^{:line 918 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count period-entries)
   actors ^{:line 919 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (distinct-actors period-entries)
   creates ^{:line 920 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 920 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action period-entries "create"))
   updates ^{:line 921 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 921 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action period-entries "update"))
   cancels ^{:line 922 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count ^{:line 922 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (entries-by-action period-entries "cancel"))]
  ^{:line 923 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:period-start start :period-end end :total-entries total :unique-actors ^{:line 923 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count actors) :creates creates :updates updates :cancels cancels}))

^{:line 936 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn orphan-invoices [invoices orders]
  ^{:line 937 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 937 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [inv] ^{:line 938 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? ^{:line 938 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/find-order-by-id orders ^{:line 938 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/invoice-order-id inv)))) invoices))

^{:line 942 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn orphan-shipments [shipments orders]
  ^{:line 943 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 943 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [s] ^{:line 944 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? ^{:line 944 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ord/find-order-by-id orders ^{:line 944 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (ship/shipment-order-id s)))) shipments))

^{:line 948 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn orphan-payments [payments invoices]
  ^{:line 949 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (filterv ^{:line 949 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (fn [p] ^{:line 950 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (nil? ^{:line 950 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/find-invoice-by-id invoices ^{:line 950 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (billing/payment-invoice-id p)))) payments))

^{:line 954 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (defn data-integrity-report [orders invoices payments shipments]
  ^{:line 956 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (let [orphan-inv ^{:line 956 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (orphan-invoices invoices orders)
   orphan-ship ^{:line 957 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (orphan-shipments shipments orders)
   orphan-pay ^{:line 958 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (orphan-payments payments invoices)
   clean ^{:line 959 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (and ^{:line 959 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 959 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-inv) 0) ^{:line 960 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 960 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-ship) 0) ^{:line 961 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (= ^{:line 961 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-pay) 0))]
  ^{:line 962 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} {:orphan-invoices ^{:line 962 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-inv) :orphan-shipments ^{:line 962 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-ship) :orphan-payments ^{:line 962 :file "/home/tom/code/beagle/experiments/e8-scaled/golden/beagle/audit.rkt"} (count orphan-pay) :data-clean clean}))
