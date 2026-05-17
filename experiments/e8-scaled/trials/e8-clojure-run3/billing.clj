(ns billing
  (:require [catalog :as cat]
            [customers :as cust]
            [orders :as ord]))

(defrecord Invoice [id order-id customer-id subtotal tax discount total status issued-at due-at paid-at])

(defn invoice-id [r] (:id r))

(defn invoice-order-id [r] (:order-id r))

(defn invoice-customer-id [r] (:customer-id r))

(defn invoice-subtotal [r] (:subtotal r))

(defn invoice-tax [r] (:tax r))

(defn invoice-discount [r] (:discount r))

(defn invoice-total [r] (:total r))

(defn invoice-status [r] (:status r))

(defn invoice-issued-at [r] (:issued-at r))

(defn invoice-due-at [r] (:due-at r))

(defn invoice-paid-at [r] (:paid-at r))

(defrecord Payment [id invoice-id amount method status processed-at])

(defn payment-id [r] (:id r))

(defn payment-invoice-id [r] (:invoice-id r))

(defn payment-amount [r] (:amount r))

(defn payment-method [r] (:method r))

(defn payment-status [r] (:status r))

(defn payment-processed-at [r] (:processed-at r))

(defrecord CreditNote [id customer-id amount reason issued-at])

(defn creditnote-id [r] (:id r))

(defn creditnote-customer-id [r] (:customer-id r))

(defn creditnote-amount [r] (:amount r))

(defn creditnote-reason [r] (:reason r))

(defn creditnote-issued-at [r] (:issued-at r))

(defn create-invoice [id order customer issued-at due-at]
  (let [sub (ord/order-subtotal order)
   tx (ord/order-tax order)
   disc (ord/order-discount order)
   tot (ord/order-total order)
   cid (cust/customer-id customer)
   oid (ord/order-id order)]
  (->Invoice id oid cid sub tx disc tot "unpaid" issued-at due-at 0)))

(defn find-invoice-by-id [invoices id]
  (first (filterv (fn [inv] (= (invoice-id inv) id)) invoices)))

(defn invoices-for-customer [invoices customer-id]
  (filterv (fn [inv] (= (invoice-customer-id inv) customer-id)) invoices))

(defn invoices-for-order [invoices order-id]
  (filterv (fn [inv] (= (invoice-order-id inv) order-id)) invoices))

(defn invoices-by-status [invoices status]
  (filterv (fn [inv] (= (invoice-status inv) status)) invoices))

(defn unpaid-invoices [invoices]
  (invoices-by-status invoices "unpaid"))

(defn paid-invoices [invoices]
  (invoices-by-status invoices "paid"))

(defn overdue-invoices [invoices now]
  (filterv (fn [inv] (and (= (invoice-status inv) "unpaid") (< (invoice-due-at inv) now))) invoices))

(defn mark-invoice-paid [inv paid-at]
  (->Invoice (invoice-id inv) (invoice-order-id inv) (invoice-customer-id inv) (invoice-subtotal inv) (invoice-tax inv) (invoice-discount inv) (invoice-total inv) "paid" (invoice-issued-at inv) (invoice-due-at inv) paid-at))

(defn invoice-age-days [inv now]
  (quot (- now (invoice-issued-at inv)) 86400))

(defn create-payment [id invoice-id amount method processed-at]
  (->Payment id invoice-id amount method "completed" processed-at))

(defn find-payment-by-id [payments id]
  (first (filterv (fn [p] (= (payment-id p) id)) payments)))

(defn payments-for-invoice [payments invoice-id]
  (filterv (fn [p] (= (payment-invoice-id p) invoice-id)) payments))

(defn total-payments-for-invoice [payments invoice-id]
  (reduce (fn [acc p] (+ acc (payment-amount p))) 0 (filterv (fn [p] (and (= (payment-invoice-id p) invoice-id) (= (payment-status p) "completed"))) payments)))

(defn invoice-balance [inv payments]
  (let [paid (total-payments-for-invoice payments (invoice-id inv))
   owed (invoice-total inv)
   bal (- owed paid)]
  (if (<= bal 0) 0 bal)))

(defn invoice-fully-paid? [inv payments]
  (= (invoice-balance inv payments) 0))

(defn payments-by-method [payments method]
  (filterv (fn [p] (= (payment-method p) method)) payments))

(defn refund-payment [p]
  (->Payment (payment-id p) (payment-invoice-id p) (payment-amount p) (payment-method p) "refunded" (payment-processed-at p)))

(defn create-credit-note [id customer-id amount reason issued-at]
  (->CreditNote id customer-id amount reason issued-at))

(defn credits-for-customer [credits customer-id]
  (filterv (fn [cn] (= (creditnote-customer-id cn) customer-id)) credits))

(defn total-credits-for-customer [credits customer-id]
  (reduce (fn [acc cn] (+ acc (creditnote-amount cn))) 0 (credits-for-customer credits customer-id)))

(defn total-revenue-collected [payments]
  (reduce (fn [acc p] (+ acc (payment-amount p))) 0 (filterv (fn [p] (= (payment-status p) "completed")) payments)))

(defn total-outstanding [invoices payments]
  (reduce (fn [acc inv] (+ acc (invoice-balance inv payments))) 0 (unpaid-invoices invoices)))

(defn customer-total-billed [invoices customer-id]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (invoices-for-customer invoices customer-id)))

(defn customer-total-paid [payments invoices customer-id]
  (let [cust-invoices (invoices-for-customer invoices customer-id)
   cust-inv-ids (mapv invoice-id cust-invoices)]
  (reduce (fn [acc p] (if (and (= (payment-status p) "completed") (some (fn [iid] (= (payment-invoice-id p) iid)) cust-inv-ids)) (+ acc (payment-amount p)) acc)) 0 payments)))

(defn revenue-by-method [payments]
  (let [completed (filterv (fn [p] (= (payment-status p) "completed")) payments)]
  (reduce (fn [acc p] (let [m (payment-method p)
   cur (get acc m)
   old (if (nil? cur) 0 cur)]
  (assoc acc m (+ old (payment-amount p))))) {} completed)))

(defn avg-days-to-pay [invoices]
  (let [pd (paid-invoices invoices)
   cnt (count pd)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc inv] (+ acc (quot (- (invoice-paid-at inv) (invoice-issued-at inv)) 86400))) 0 pd) cnt))))

(defn collection-rate-pct [invoices payments]
  (let [total-invoiced (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 invoices)
   collected (total-revenue-collected payments)]
  (if (= total-invoiced 0) 100 (quot (* collected 100) total-invoiced))))

(defn invoice-summary [inv]
  (str "Invoice #" (invoice-id inv) " | Order #" (invoice-order-id inv) " | " (invoice-status inv) " | Total: " (cat/format-price (invoice-total inv))))

(defn invoice-detail [inv]
  (str "Invoice #" (invoice-id inv) " | Customer: " (invoice-customer-id inv) " | Sub: " (cat/format-price (invoice-subtotal inv)) " | Tax: " (cat/format-price (invoice-tax inv)) " | Disc: " (cat/format-price (invoice-discount inv)) " | Total: " (cat/format-price (invoice-total inv)) " | Status: " (invoice-status inv)))

(defn payment-summary [p]
  (str "Payment #" (payment-id p) " | Invoice #" (payment-invoice-id p) " | " (cat/format-price (payment-amount p)) " | " (payment-method p) " | " (payment-status p)))

(defn credit-note-summary [cn]
  (str "Credit #" (creditnote-id cn) " | Customer: " (creditnote-customer-id cn) " | " (cat/format-price (creditnote-amount cn)) " | " (creditnote-reason cn)))

(defn valid-invoice? [inv]
  (and (> (invoice-id inv) 0) (> (invoice-order-id inv) 0) (> (invoice-customer-id inv) 0) (>= (invoice-total inv) 0) (not= (invoice-status inv) "")))

(defn valid-payment? [p]
  (and (> (payment-id p) 0) (> (payment-invoice-id p) 0) (> (payment-amount p) 0) (not= (payment-method p) "")))

(defn valid-credit-note? [cn]
  (and (> (creditnote-id cn) 0) (> (creditnote-customer-id cn) 0) (> (creditnote-amount cn) 0) (not= (creditnote-reason cn) "")))

(defn sort-invoices-by-total [invoices]
  (sort-by invoice-total invoices))

(defn sort-invoices-by-date [invoices]
  (sort-by invoice-issued-at invoices))

(defn sort-invoices-by-due [invoices]
  (sort-by invoice-due-at invoices))

(defn sort-payments-by-date [payments]
  (sort-by payment-processed-at payments))

(defn invoices-in-period [invoices start-ts end-ts]
  (filterv (fn [inv] (and (>= (invoice-issued-at inv) start-ts) (<= (invoice-issued-at inv) end-ts))) invoices))

(defn payments-in-period [payments start-ts end-ts]
  (filterv (fn [p] (and (>= (payment-processed-at p) start-ts) (<= (payment-processed-at p) end-ts))) payments))

(defn invoices-for-amount-range [invoices lo hi]
  (filterv (fn [inv] (and (>= (invoice-total inv) lo) (<= (invoice-total inv) hi))) invoices))

(defn customer-invoice-count [invoices customer-id]
  (count (invoices-for-customer invoices customer-id)))

(defn customer-unpaid-count [invoices customer-id]
  (count (filterv (fn [inv] (and (= (invoice-customer-id inv) customer-id) (= (invoice-status inv) "unpaid"))) invoices)))

(defn customer-outstanding-balance [invoices payments customer-id]
  (let [cust-unpaid (filterv (fn [inv] (and (= (invoice-customer-id inv) customer-id) (= (invoice-status inv) "unpaid"))) invoices)]
  (reduce (fn [acc inv] (+ acc (invoice-balance inv payments))) 0 cust-unpaid)))

(defn customer-avg-invoice-value [invoices customer-id]
  (let [cust-invs (invoices-for-customer invoices customer-id)
   cnt (count cust-invs)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 cust-invs) cnt))))

(defn payment-count-by-method [payments method]
  (count (filterv (fn [p] (and (= (payment-method p) method) (= (payment-status p) "completed"))) payments)))

(defn total-refunded [payments]
  (reduce (fn [acc p] (+ acc (payment-amount p))) 0 (filterv (fn [p] (= (payment-status p) "refunded")) payments)))

(defn net-revenue [payments]
  (- (total-revenue-collected payments) (total-refunded payments)))

(defn completed-payment-count [payments]
  (count (filterv (fn [p] (= (payment-status p) "completed")) payments)))

(defn avg-payment-amount [payments]
  (let [completed (filterv (fn [p] (= (payment-status p) "completed")) payments)
   cnt (count completed)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc p] (+ acc (payment-amount p))) 0 completed) cnt))))

(defn total-credits-issued [credits]
  (reduce (fn [acc cn] (+ acc (creditnote-amount cn))) 0 credits))

(defn credit-note-count [credits]
  (count credits))

(defn customer-credit-count [credits customer-id]
  (count (credits-for-customer credits customer-id)))

(defn avg-credit-note-amount [credits]
  (let [cnt (count credits)]
  (if (= cnt 0) 0 (quot (total-credits-issued credits) cnt))))

(defn invoice-matches-order? [inv orders]
  (let [o (ord/find-order-by-id orders (invoice-order-id inv))]
  (if (nil? o) false (= (invoice-total inv) (ord/order-total o)))))

(defn invoice-order-status [inv orders]
  (let [o (ord/find-order-by-id orders (invoice-order-id inv))]
  (if (nil? o) "unknown" (ord/order-status o))))

(defn customer-billing-summary [invoices payments credits customer-id]
  (let [billed (customer-total-billed invoices customer-id)
   paid (customer-total-paid payments invoices customer-id)
   outstanding (customer-outstanding-balance invoices payments customer-id)
   credit-amt (total-credits-for-customer credits customer-id)]
  (str "Customer #" customer-id " | Billed: " (cat/format-price billed) " | Paid: " (cat/format-price paid) " | Outstanding: " (cat/format-price outstanding) " | Credits: " (cat/format-price credit-amt))))

(defn invoice-age-bucket [inv now]
  (let [days (invoice-age-days inv now)]
  (cond
  (< days 30) "current"
  (< days 60) "30-60"
  (< days 90) "60-90"
  true "90+")))

(defn aging-report [invoices now]
  (let [unpaid (unpaid-invoices invoices)
   current-cnt (count (filterv (fn [inv] (= (invoice-age-bucket inv now) "current")) unpaid))
   d30-cnt (count (filterv (fn [inv] (= (invoice-age-bucket inv now) "30-60")) unpaid))
   d60-cnt (count (filterv (fn [inv] (= (invoice-age-bucket inv now) "60-90")) unpaid))
   d90-cnt (count (filterv (fn [inv] (= (invoice-age-bucket inv now) "90+")) unpaid))]
  {:current current-cnt :30-60 d30-cnt :60-90 d60-cnt :90+ d90-cnt}))

(defn aging-total [invoices payments now]
  (let [unpaid (unpaid-invoices invoices)
   bucket-total (fn [bucket-name] (reduce (fn [acc inv] (if (= (invoice-age-bucket inv now) bucket-name) (+ acc (invoice-balance inv payments)) acc)) 0 unpaid))]
  {:current (bucket-total "current") :30-60 (bucket-total "30-60") :60-90 (bucket-total "60-90") :90+ (bucket-total "90+")}))

(defn bulk-create-invoices [specs]
  (mapv (fn [spec] (let [id (nth spec 0)
   order (nth spec 1)
   customer (nth spec 2)
   issued (nth spec 3)
   due (nth spec 4)]
  (create-invoice id order customer issued due))) specs))

(defn overdue-invoice-ids [invoices now]
  (mapv invoice-id (overdue-invoices invoices now)))

(defn largest-invoices [invoices n]
  (vec (take n (reverse (sort-invoices-by-total invoices)))))

(defn largest-payments [payments n]
  (vec (take n (reverse (sort-by payment-amount payments)))))

(defn most-indebted-customer-ids [invoices payments]
  (let [unpaid (unpaid-invoices invoices)
   cids (distinct (mapv invoice-customer-id unpaid))]
  (reverse (sort-by (fn [cid] (customer-outstanding-balance invoices payments cid)) cids))))

(defn sum-invoice-totals-in-period [invoices start-ts end-ts]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (invoices-in-period invoices start-ts end-ts)))

(defn sum-payments-in-period [payments start-ts end-ts]
  (reduce (fn [acc p] (+ acc (payment-amount p))) 0 (filterv (fn [p] (= (payment-status p) "completed")) (payments-in-period payments start-ts end-ts))))

(defn invoice-payment-gap [invoices payments start-ts end-ts]
  (- (sum-invoice-totals-in-period invoices start-ts end-ts) (sum-payments-in-period payments start-ts end-ts)))

(defn days-past-due [inv now]
  (let [diff (- (invoice-due-at inv) now)]
  (if (<= diff 0) 0 (quot diff 86400))))

(defn severely-overdue [invoices now threshold-days]
  (filterv (fn [inv] (and (= (invoice-status inv) "unpaid") (> (days-past-due inv now) threshold-days))) invoices))

(defn overdue-total [invoices now]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (overdue-invoices invoices now)))

(defn avg-days-past-due [invoices now]
  (let [unpaid (unpaid-invoices invoices)
   cnt (count unpaid)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc inv] (+ acc (days-past-due inv now))) 0 unpaid) cnt))))

(defn apply-payment-to-invoice [inv payments pay-id amount method processed-at]
  (let [bal (invoice-balance inv payments)
   applied (if (> amount bal) bal amount)
   remaining (- amount applied)
   new-payment (create-payment pay-id (invoice-id inv) applied method processed-at)
   new-bal (- bal applied)
   new-inv (if (= new-bal 0) (mark-invoice-paid inv processed-at) inv)]
  {:invoice new-inv :payment new-payment :remaining remaining}))

(defn split-payment-across-invoices [invoices payments start-pay-id total-amount method processed-at]
  (let [sorted-unpaid (sort-invoices-by-date (unpaid-invoices invoices))]
  (first (reduce (fn [state inv] (let [created (first state)
   remaining (second state)
   next-id (nth state 2)]
  (if (<= remaining 0) state (let [bal (invoice-balance inv payments)
   applied (if (> remaining bal) bal remaining)
   pay (create-payment next-id (invoice-id inv) applied method processed-at)
   left (- remaining applied)]
  [(conj created pay) left (+ next-id 1)])))) [[] total-amount start-pay-id] sorted-unpaid))))

(defn invoice-lifecycle-status [inv payments now]
  (cond
  (= (invoice-status inv) "paid") "paid"
  (invoice-fully-paid? inv payments) "paid-pending-mark"
  (> (days-past-due inv now) 90) "severely-overdue"
  (> (days-past-due inv now) 0) "overdue"
  true "current"))

(defn invoice-health-score [inv payments now]
  (let [status (invoice-lifecycle-status inv payments now)]
  (cond
  (= status "paid") 100
  (= status "paid-pending-mark") 95
  (= status "current") 80
  (= status "overdue") (let [dpd (days-past-due inv now)]
  (if (> dpd 90) 0 (- 60 (quot (* dpd 60) 90))))
  true 0)))

(defn portfolio-health [invoices payments now]
  (let [cnt (count invoices)]
  (if (= cnt 0) 100 (quot (reduce (fn [acc inv] (+ acc (invoice-health-score inv payments now))) 0 invoices) cnt))))

(defn recognized-revenue [invoices]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (paid-invoices invoices)))

(defn unrecognized-revenue [invoices]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (unpaid-invoices invoices)))

(defn deferred-revenue-pct [invoices]
  (let [total (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 invoices)]
  (if (= total 0) 0 (quot (* (unrecognized-revenue invoices) 100) total))))

(defn customer-net-position [invoices payments credits customer-id]
  (let [billed (customer-total-billed invoices customer-id)
   paid (customer-total-paid payments invoices customer-id)
   cred (total-credits-for-customer credits customer-id)]
  (- billed (+ paid cred))))

(defn customer-has-credit? [credits customer-id]
  (> (total-credits-for-customer credits customer-id) 0))

(defn customer-lifetime-value [payments invoices customer-id]
  (customer-total-paid payments invoices customer-id))

(defn customer-payment-velocity [invoices customer-id]
  (let [cust-paid (filterv (fn [inv] (and (= (invoice-customer-id inv) customer-id) (= (invoice-status inv) "paid"))) invoices)
   cnt (count cust-paid)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc inv] (+ acc (quot (- (invoice-paid-at inv) (invoice-issued-at inv)) 86400))) 0 cust-paid) cnt))))

(defn total-tax-billed [invoices]
  (reduce (fn [acc inv] (+ acc (invoice-tax inv))) 0 invoices))

(defn total-discounts-billed [invoices]
  (reduce (fn [acc inv] (+ acc (invoice-discount inv))) 0 invoices))

(defn effective-tax-rate [invoices]
  (let [total-sub (reduce (fn [acc inv] (+ acc (invoice-subtotal inv))) 0 invoices)]
  (if (= total-sub 0) 0 (quot (* (total-tax-billed invoices) 100) total-sub))))

(defn effective-discount-rate [invoices]
  (let [total-sub (reduce (fn [acc inv] (+ acc (invoice-subtotal inv))) 0 invoices)]
  (if (= total-sub 0) 0 (quot (* (total-discounts-billed invoices) 100) total-sub))))

(defn revenue-collected-in-period [payments start-ts end-ts]
  (reduce (fn [acc p] (+ acc (payment-amount p))) 0 (filterv (fn [p] (and (= (payment-status p) "completed") (>= (payment-processed-at p) start-ts) (<= (payment-processed-at p) end-ts))) payments)))

(defn invoiced-in-period [invoices start-ts end-ts]
  (sum-invoice-totals-in-period invoices start-ts end-ts))

(defn period-collection-rate [invoices payments start-ts end-ts]
  (let [issued (invoiced-in-period invoices start-ts end-ts)
   collected (revenue-collected-in-period payments start-ts end-ts)]
  (if (= issued 0) 100 (quot (* collected 100) issued))))

(defn orders-without-invoices [orders invoices]
  (let [invoiced-order-ids (mapv invoice-order-id invoices)]
  (filterv (fn [o] (not (some (fn [ioid] (= (ord/order-id o) ioid)) invoiced-order-ids))) orders)))

(defn invoices-without-orders [invoices orders]
  (filterv (fn [inv] (nil? (ord/find-order-by-id orders (invoice-order-id inv)))) invoices))

(defn duplicate-invoices-for-order [invoices order-id]
  (let [matches (invoices-for-order invoices order-id)]
  (if (> (count matches) 1) matches [])))

(defn billing-summary [invoices payments credits now]
  (let [total-billed (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 invoices)
   collected (total-revenue-collected payments)
   outstanding (total-outstanding invoices payments)
   overdue-amt (overdue-total invoices now)
   refunded (total-refunded payments)
   credited (total-credits-issued credits)
   inv-count (count invoices)
   paid-count (count (paid-invoices invoices))
   unpaid-count (count (unpaid-invoices invoices))
   overdue-count (count (overdue-invoices invoices now))]
  {:total-billed total-billed :total-collected collected :total-outstanding outstanding :total-overdue overdue-amt :total-refunded refunded :total-credits credited :invoice-count inv-count :paid-count paid-count :unpaid-count unpaid-count :overdue-count overdue-count :collection-rate (collection-rate-pct invoices payments) :health-score (portfolio-health invoices payments now)}))

(defn customer-billing-snapshot [invoices payments credits customer-id now]
  (let [billed (customer-total-billed invoices customer-id)
   paid (customer-total-paid payments invoices customer-id)
   outstanding (customer-outstanding-balance invoices payments customer-id)
   credit-amt (total-credits-for-customer credits customer-id)
   net-pos (customer-net-position invoices payments credits customer-id)
   inv-cnt (customer-invoice-count invoices customer-id)
   unpaid-cnt (customer-unpaid-count invoices customer-id)
   velocity (customer-payment-velocity invoices customer-id)]
  {:customer-id customer-id :total-billed billed :total-paid paid :outstanding outstanding :credits credit-amt :net-position net-pos :invoice-count inv-cnt :unpaid-count unpaid-cnt :avg-days-to-pay velocity}))

(defn method-payment-count-map [payments]
  (let [completed (filterv (fn [p] (= (payment-status p) "completed")) payments)]
  (reduce (fn [acc p] (let [m (payment-method p)
   cur (get acc m)
   old (if (nil? cur) 0 cur)]
  (assoc acc m (+ old 1)))) {} completed)))

(defn method-avg-payment [payments method]
  (let [matching (filterv (fn [p] (and (= (payment-method p) method) (= (payment-status p) "completed"))) payments)
   cnt (count matching)]
  (if (= cnt 0) 0 (quot (reduce (fn [acc p] (+ acc (payment-amount p))) 0 matching) cnt))))

(defn candidates-for-write-off [invoices payments now threshold-days]
  (filterv (fn [inv] (and (= (invoice-status inv) "unpaid") (> (invoice-age-days inv now) threshold-days) (= (total-payments-for-invoice payments (invoice-id inv)) 0))) invoices))

(defn write-off-exposure [invoices payments now threshold-days]
  (reduce (fn [acc inv] (+ acc (invoice-total inv))) 0 (candidates-for-write-off invoices payments now threshold-days)))

(defn partial-payment-invoices [invoices payments]
  (filterv (fn [inv] (let [bal (invoice-balance inv payments)
   tot (invoice-total inv)]
  (and (> bal 0) (< bal tot)))) invoices))

(defn customer-statement-lines [invoices payments customer-id]
  (let [cust-invs (sort-invoices-by-date (invoices-for-customer invoices customer-id))]
  (mapv (fn [inv] (let [bal (invoice-balance inv payments)]
  (str "Invoice #" (invoice-id inv) " | Issued: " (invoice-issued-at inv) " | Due: " (invoice-due-at inv) " | Total: " (cat/format-price (invoice-total inv)) " | Paid: " (cat/format-price (- (invoice-total inv) bal)) " | Balance: " (cat/format-price bal) " | Status: " (invoice-status inv)))) cust-invs)))

(defn payment-receipt [p]
  (str "=== Payment Receipt ===" "\nPayment ID: " (payment-id p) "\nInvoice: #" (payment-invoice-id p) "\nAmount: " (cat/format-price (payment-amount p)) "\nMethod: " (payment-method p) "\nStatus: " (payment-status p) "\nProcessed: " (payment-processed-at p) "\n========================"))
