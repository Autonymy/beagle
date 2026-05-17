(ns procurement
  (:require [catalog :as cat]
            [inventory :as inv]))

(defrecord PurchaseOrder [id supplier-id status created-at expected-at total])

(defn purchaseorder-id [r] (:id r))

(defn purchaseorder-supplier-id [r] (:supplier-id r))

(defn purchaseorder-status [r] (:status r))

(defn purchaseorder-created-at [r] (:created-at r))

(defn purchaseorder-expected-at [r] (:expected-at r))

(defn purchaseorder-total [r] (:total r))

(defrecord POLine [po-id product-id quantity unit-cost])

(defn poline-po-id [r] (:po-id r))

(defn poline-product-id [r] (:product-id r))

(defn poline-quantity [r] (:quantity r))

(defn poline-unit-cost [r] (:unit-cost r))

(defrecord GoodsReceipt [id po-id warehouse-id received-at line-count])

(defn goodsreceipt-id [r] (:id r))

(defn goodsreceipt-po-id [r] (:po-id r))

(defn goodsreceipt-warehouse-id [r] (:warehouse-id r))

(defn goodsreceipt-received-at [r] (:received-at r))

(defn goodsreceipt-line-count [r] (:line-count r))

(defn poline-total [line]
  (* (poline-quantity line) (poline-unit-cost line)))

(defn po-total-from-lines [lines]
  (reduce (fn [acc line] (+ acc (poline-total line))) 0 lines))

(defn po-line-count [lines po-id]
  (count (filterv (fn [line] (= (poline-po-id line) po-id)) lines)))

(defn lines-for-po [lines po-id]
  (filterv (fn [line] (= (poline-po-id line) po-id)) lines))

(defn lines-for-product [lines product-id]
  (filterv (fn [line] (= (poline-product-id line) product-id)) lines))

(defn total-quantity-for-product [lines product-id]
  (reduce (fn [acc line] (+ acc (poline-quantity line))) 0 (lines-for-product lines product-id)))

(defn total-spend-for-product [lines product-id]
  (reduce (fn [acc line] (+ acc (poline-total line))) 0 (lines-for-product lines product-id)))

(defn avg-unit-cost-for-product [lines product-id]
  (let [prod-lines (lines-for-product lines product-id)
   cnt (count prod-lines)]
  (if (= cnt 0) 0 (quot (total-spend-for-product lines product-id) (total-quantity-for-product lines product-id)))))

(defn unique-products-in-lines [lines]
  (distinct (mapv poline-product-id lines)))

(defn create-purchase-order [id supplier-id lines created-at expected-at]
  (let [total (po-total-from-lines lines)]
  (->PurchaseOrder id supplier-id "pending" created-at expected-at total)))

(defn find-po-by-id [pos id]
  (first (filterv (fn [po] (= (purchaseorder-id po) id)) pos)))

(defn pos-for-supplier [pos supplier-id]
  (filterv (fn [po] (= (purchaseorder-supplier-id po) supplier-id)) pos))

(defn pos-by-status [pos status]
  (filterv (fn [po] (= (purchaseorder-status po) status)) pos))

(defn pending-pos [pos]
  (filterv (fn [po] (= (purchaseorder-status po) "pending")) pos))

(defn approved-pos [pos]
  (filterv (fn [po] (= (purchaseorder-status po) "approved")) pos))

(defn completed-pos [pos]
  (filterv (fn [po] (= (purchaseorder-status po) "completed")) pos))

(defn cancelled-pos [pos]
  (filterv (fn [po] (= (purchaseorder-status po) "cancelled")) pos))

(defn pos-in-period [pos start end]
  (filterv (fn [po] (and (>= (purchaseorder-created-at po) start) (<= (purchaseorder-created-at po) end))) pos))

(defn sort-pos-by-total [pos]
  (reverse (sort-by purchaseorder-total pos)))

(defn sort-pos-by-date [pos]
  (sort-by purchaseorder-created-at pos))

(defn approve-po [po]
  (if (= (purchaseorder-status po) "pending") (->PurchaseOrder (purchaseorder-id po) (purchaseorder-supplier-id po) "approved" (purchaseorder-created-at po) (purchaseorder-expected-at po) (purchaseorder-total po)) po))

(defn complete-po [po]
  (->PurchaseOrder (purchaseorder-id po) (purchaseorder-supplier-id po) "completed" (purchaseorder-created-at po) (purchaseorder-expected-at po) (purchaseorder-total po)))

(defn cancel-po [po]
  (if (= (purchaseorder-status po) "pending") (->PurchaseOrder (purchaseorder-id po) (purchaseorder-supplier-id po) "cancelled" (purchaseorder-created-at po) (purchaseorder-expected-at po) (purchaseorder-total po)) po))

(defn po-open? [po]
  (or (= (purchaseorder-status po) "pending") (= (purchaseorder-status po) "approved")))

(defn po-closed? [po]
  (or (= (purchaseorder-status po) "completed") (= (purchaseorder-status po) "cancelled")))

(defn po-expected-lead-days [po]
  (let [elapsed (- (purchaseorder-expected-at po) (purchaseorder-created-at po))]
  (quot elapsed 86400)))

(defn valid-po? [po]
  (and (> (purchaseorder-id po) 0) (> (purchaseorder-supplier-id po) 0) (not= (purchaseorder-status po) "") (>= (purchaseorder-total po) 0)))

(defn valid-poline? [line]
  (and (> (poline-product-id line) 0) (> (poline-quantity line) 0) (>= (poline-unit-cost line) 0)))

(defn valid-lines [lines]
  (filterv valid-poline? lines))

(defn all-lines-valid? [lines]
  (every? valid-poline? lines))

(defn create-goods-receipt [id po-id warehouse-id received-at lines]
  (let [lc (count (filterv (fn [line] (= (poline-po-id line) po-id)) lines))]
  (->GoodsReceipt id po-id warehouse-id received-at lc)))

(defn receipts-for-po [receipts po-id]
  (filterv (fn [r] (= (goodsreceipt-po-id r) po-id)) receipts))

(defn receipts-for-warehouse [receipts warehouse-id]
  (filterv (fn [r] (= (goodsreceipt-warehouse-id r) warehouse-id)) receipts))

(defn find-receipt-by-id [receipts id]
  (first (filterv (fn [r] (= (goodsreceipt-id r) id)) receipts)))

(defn receipts-in-period [receipts start end]
  (filterv (fn [r] (and (>= (goodsreceipt-received-at r) start) (<= (goodsreceipt-received-at r) end))) receipts))

(defn total-lines-received-for-po [receipts po-id]
  (reduce (fn [acc r] (+ acc (goodsreceipt-line-count r))) 0 (receipts-for-po receipts po-id)))

(defn po-has-receipt? [receipts po-id]
  (not (empty? (receipts-for-po receipts po-id))))

(defn warehouse-receipt-count [receipts warehouse-id]
  (count (receipts-for-warehouse receipts warehouse-id)))

(defn supplier-total-ordered [pos lines supplier-id]
  (let [supplier-pos (pos-for-supplier pos supplier-id)
   supplier-po-ids (mapv (fn [po] (purchaseorder-id po)) supplier-pos)
   matching-lines (filterv (fn [line] (some (fn [pid] (= pid (poline-po-id line))) supplier-po-ids)) lines)]
  (reduce (fn [acc line] (+ acc (poline-total line))) 0 matching-lines)))

(defn supplier-order-count [pos supplier-id]
  (count (pos-for-supplier pos supplier-id)))

(defn supplier-avg-order-value [pos supplier-id]
  (let [spos (pos-for-supplier pos supplier-id)
   cnt (count spos)]
  (if (= cnt 0) 0 (let [total (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 spos)]
  (quot total cnt)))))

(defn supplier-completed-count [pos supplier-id]
  (count (filterv (fn [po] (= (purchaseorder-status po) "completed")) (pos-for-supplier pos supplier-id))))

(defn supplier-completion-rate [pos supplier-id]
  (let [spos (pos-for-supplier pos supplier-id)
   total (count spos)
   completed (count (filterv (fn [po] (= (purchaseorder-status po) "completed")) spos))]
  (if (= total 0) 0 (quot (* completed 100) total))))

(defn supplier-cancellation-rate [pos supplier-id]
  (let [spos (pos-for-supplier pos supplier-id)
   total (count spos)
   cancelled (count (filterv (fn [po] (= (purchaseorder-status po) "cancelled")) spos))]
  (if (= total 0) 0 (quot (* cancelled 100) total))))

(defn supplier-completed-spend [pos supplier-id]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 (filterv (fn [po] (= (purchaseorder-status po) "completed")) (pos-for-supplier pos supplier-id))))

(defn overdue-pos [pos now]
  (filterv (fn [po] (and (po-open? po) (< (purchaseorder-expected-at po) now))) pos))

(defn overdue-count [pos now]
  (count (overdue-pos pos now)))

(defn overdue-total [pos now]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 (overdue-pos pos now)))

(defn po-days-overdue [po now]
  (let [diff (- now (purchaseorder-expected-at po))]
  (if (> diff 0) (quot diff 86400) 0)))

(defn auto-reorder-lines [levels products]
  (let [needed (inv/reorder-needed levels)]
  (mapv (fn [sl] (let [prod-id (inv/stocklevel-product-id sl)
   qty (inv/reorder-quantity sl)
   prod (cat/find-product-by-id products prod-id)
   cost (if (nil? prod) 0 (cat/product-unit-cost prod))]
  (->POLine 0 prod-id qty cost))) needed)))

(defn auto-reorder-cost [levels products]
  (po-total-from-lines (auto-reorder-lines levels products)))

(defn auto-reorder-product-count [levels]
  (count (inv/reorder-needed levels)))

(defn total-procurement-spend [pos]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 (filterv (fn [po] (= (purchaseorder-status po) "completed")) pos)))

(defn open-procurement-value [pos]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 (filterv po-open? pos)))

(defn gross-procurement-value [pos]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 pos))

(defn avg-po-value [pos]
  (let [cnt (count pos)]
  (if (= cnt 0) 0 (quot (gross-procurement-value pos) cnt))))

(defn period-procurement-spend [pos start end]
  (reduce (fn [acc po] (+ acc (purchaseorder-total po))) 0 (filterv (fn [po] (and (= (purchaseorder-status po) "completed") (>= (purchaseorder-created-at po) start) (<= (purchaseorder-created-at po) end))) pos)))

(defn avg-lead-time-actual [pos receipts]
  (let [completed (filterv (fn [po] (= (purchaseorder-status po) "completed")) pos)
   with-receipt (filterv (fn [po] (not (empty? (receipts-for-po receipts (purchaseorder-id po))))) completed)]
  (if (empty? with-receipt) 0 (let [total-days (reduce (fn [acc po] (let [po-receipts (receipts-for-po receipts (purchaseorder-id po))
   first-receipt (first po-receipts)
   elapsed (- (goodsreceipt-received-at first-receipt) (purchaseorder-created-at po))
   days (quot elapsed 86400)]
  (+ acc days))) 0 with-receipt)]
  (quot total-days (count with-receipt))))))

(defn po-actual-lead-days [po receipts]
  (let [po-receipts (receipts-for-po receipts (purchaseorder-id po))]
  (if (empty? po-receipts) 0 (let [first-receipt (first po-receipts)
   elapsed (- (goodsreceipt-received-at first-receipt) (purchaseorder-created-at po))]
  (quot elapsed 86400)))))

(defn po-lead-variance-days [po receipts]
  (let [actual (po-actual-lead-days po receipts)
   expected (po-expected-lead-days po)]
  (- actual expected)))

(defn max-lead-time-actual [pos receipts]
  (let [completed (filterv (fn [po] (= (purchaseorder-status po) "completed")) pos)
   with-receipt (filterv (fn [po] (not (empty? (receipts-for-po receipts (purchaseorder-id po))))) completed)]
  (if (empty? with-receipt) 0 (reduce (fn [mx po] (let [days (po-actual-lead-days po receipts)]
  (if (> days mx) days mx))) 0 with-receipt))))

(defn po-summary [po]
  (str "PO#" (purchaseorder-id po) " supplier:" (purchaseorder-supplier-id po) " status:" (purchaseorder-status po) " total:" (purchaseorder-total po)))

(defn poline-summary [line]
  (str "POLine po:" (poline-po-id line) " product:" (poline-product-id line) " qty:" (poline-quantity line) " @" (poline-unit-cost line) " = " (poline-total line)))

(defn receipt-summary [r]
  (str "GR#" (goodsreceipt-id r) " po:" (goodsreceipt-po-id r) " wh:" (goodsreceipt-warehouse-id r) " lines:" (goodsreceipt-line-count r)))

(defn po-detail [po lines products]
  (let [po-lines (lines-for-po lines (purchaseorder-id po))
   line-strs (mapv (fn [line] (let [prod (cat/find-product-by-id products (poline-product-id line))
   pname (if (nil? prod) "unknown" (cat/product-name prod))]
  (str "  " pname " qty:" (poline-quantity line) " @" (poline-unit-cost line)))) po-lines)]
  (str (po-summary po) "\n" (reduce (fn [acc s] (str acc s "\n")) "" line-strs))))

(defn enrich-lines [lines products levels]
  (mapv (fn [line] (let [prod-id (poline-product-id line)
   prod (cat/find-product-by-id products prod-id)
   pname (if (nil? prod) "unknown" (cat/product-name prod))
   stock (inv/total-stock-for-product levels prod-id)]
  {:po-id (poline-po-id line) :product-id prod-id :product-name pname :quantity (poline-quantity line) :unit-cost (poline-unit-cost line) :line-total (poline-total line) :current-stock stock})) lines))

(defn orphan-line-product-ids [lines products]
  (filterv (fn [pid] (nil? (cat/find-product-by-id products pid))) (unique-products-in-lines lines)))

(defn po-count-by-status [pos]
  {:pending (count (pending-pos pos)) :approved (count (approved-pos pos)) :completed (count (completed-pos pos)) :cancelled (count (cancelled-pos pos))})

(defn total-line-count [lines]
  (count lines))

(defn total-units-ordered [lines]
  (reduce (fn [acc line] (+ acc (poline-quantity line))) 0 lines))

(defn avg-line-value [lines]
  (let [cnt (count lines)]
  (if (= cnt 0) 0 (quot (po-total-from-lines lines) cnt))))

(defn avg-line-quantity [lines]
  (let [cnt (count lines)]
  (if (= cnt 0) 0 (quot (total-units-ordered lines) cnt))))

(defn largest-po [pos]
  (if (empty? pos) nil (reduce (fn [best po] (if (> (purchaseorder-total po) (purchaseorder-total best)) po best)) (first pos) (rest pos))))

(defn smallest-active-po [pos]
  (let [active (filterv (fn [po] (not= (purchaseorder-status po) "cancelled")) pos)]
  (if (empty? active) nil (reduce (fn [best po] (if (< (purchaseorder-total po) (purchaseorder-total best)) po best)) (first active) (rest active)))))

(defn cost-variance [lines products product-id]
  (let [avg-po-cost (avg-unit-cost-for-product lines product-id)
   prod (cat/find-product-by-id products product-id)
   cat-cost (if (nil? prod) 0 (cat/product-unit-cost prod))]
  (- avg-po-cost cat-cost)))

(defn products-over-catalog-cost [lines products]
  (let [pids (unique-products-in-lines lines)]
  (filterv (fn [pid] (let [variance (cost-variance lines products pid)]
  (> variance 0))) pids)))

(defn warehouse-received-value [pos receipts lines warehouse-id]
  (let [wh-receipts (receipts-for-warehouse receipts warehouse-id)]
  (reduce (fn [acc r] (let [po (find-po-by-id pos (goodsreceipt-po-id r))
   po-total (if (nil? po) 0 (purchaseorder-total po))]
  (+ acc po-total))) 0 wh-receipts)))

(defn procurement-dashboard [pos lines receipts now]
  (let [total-spend (total-procurement-spend pos)
   open-value (open-procurement-value pos)
   overdue-cnt (overdue-count pos now)
   overdue-val (overdue-total pos now)
   avg-lead (avg-lead-time-actual pos receipts)
   po-cnt (count pos)
   line-cnt (total-line-count lines)
   units (total-units-ordered lines)]
  {:total-spend total-spend :open-value open-value :overdue-count overdue-cnt :overdue-value overdue-val :avg-lead-time-days avg-lead :po-count po-cnt :line-count line-cnt :total-units units}))
