(ns master.verify
  (:require [catalog :as cat]
            [customers :as cust]
            [inventory :as inv]
            [orders :as ord]
            [reports :as rep]))

;; ---------------------------------------------------------------------------
;; Assertion helpers
;; ---------------------------------------------------------------------------

(def ^:dynamic *pass-count* (atom 0))
(def ^:dynamic *fail-count* (atom 0))

(defn assert-eq [label expected actual]
  (if (= expected actual)
    (swap! *pass-count* inc)
    (do (swap! *fail-count* inc)
        (println (str "FAIL: " label "\n  expected: " expected "\n  actual:   " actual)))))

(defn assert-true [label val]
  (assert-eq label true val))

(defn assert-false [label val]
  (assert-eq label false val))

;; ---------------------------------------------------------------------------
;; Test Data
;; ---------------------------------------------------------------------------

;; Suppliers
(def sup1 (cat/->Supplier 1 "Acme Corp" 7))
(def sup2 (cat/->Supplier 2 "Widget Co" 14))
(def sup3 (cat/->Supplier 3 "Parts R Us" 3))
(def suppliers [sup1 sup2 sup3])

;; Categories
(def cat1 (cat/->Category 1 "Electronics" 0.15))
(def cat2 (cat/->Category 2 "Office" 0.08))
(def cat3 (cat/->Category 3 "Industrial" 0.12))
(def categories [cat1 cat2 cat3])

;; Products
(def prod1 (cat/->Product 1 "Widget A" "WGT-001" 500 1200 1 1 true))
(def prod2 (cat/->Product 2 "Gadget B" "GDG-002" 800 1500 1 1 true))
(def prod3 (cat/->Product 3 "Stapler" "STP-003" 200 450 2 2 true))
(def prod4 (cat/->Product 4 "Desk Lamp" "DLM-004" 350 750 2 2 true))
(def prod5 (cat/->Product 5 "Bolt Pack" "BLT-005" 50 120 3 3 true))
(def prod6 (cat/->Product 6 "Gear Set" "GRS-006" 1200 2200 3 3 true))
(def prod7 (cat/->Product 7 "Old Widget" "OWG-007" 300 600 1 1 false))
(def products [prod1 prod2 prod3 prod4 prod5 prod6 prod7])

;; Warehouses
(def wh1 (inv/->Warehouse 1 "Main DC" "Chicago" 10000))
(def wh2 (inv/->Warehouse 2 "West Hub" "Portland" 5000))
(def warehouses [wh1 wh2])

;; Stock levels
(def sl-1-1 (inv/->StockLevel 1 1 100 20))
(def sl-1-2 (inv/->StockLevel 1 2 50 15))
(def sl-1-3 (inv/->StockLevel 1 3 200 50))
(def sl-1-4 (inv/->StockLevel 1 4 30 10))
(def sl-1-5 (inv/->StockLevel 1 5 500 100))
(def sl-1-6 (inv/->StockLevel 1 6 8 10))   ;; below min
(def sl-2-1 (inv/->StockLevel 2 1 75 20))
(def sl-2-2 (inv/->StockLevel 2 2 25 10))
(def sl-2-3 (inv/->StockLevel 2 3 0 30))   ;; out of stock
(def sl-2-5 (inv/->StockLevel 2 5 300 50))
(def stock-levels [sl-1-1 sl-1-2 sl-1-3 sl-1-4 sl-1-5 sl-1-6
                   sl-2-1 sl-2-2 sl-2-3 sl-2-5])

;; Customers
(def addr1 (cust/->Address "123 Main St" "Chicago" "IL" "60601"))
(def addr2 (cust/->Address "456 Oak Ave" "Portland" "OR" "97201"))
(def addr3 (cust/->Address "789 Pine Rd" "Austin" "TX" "78701"))

(def cust1 (cust/->Customer 1 "Alice Smith" "alice@example.com" "gold" addr1 2020))
(def cust2 (cust/->Customer 2 "Bob Jones" "bob@example.com" "silver" addr2 2021))
(def cust3 (cust/->Customer 3 "Carol White" "carol@example.com" "bronze" addr3 2023))
(def customers [cust1 cust2 cust3])

;; Purchase records
(def pr1 (cust/->PurchaseRecord 1 101 15000 2024))
(def pr2 (cust/->PurchaseRecord 1 102 22000 2024))
(def pr3 (cust/->PurchaseRecord 1 103 18000 2025))
(def pr4 (cust/->PurchaseRecord 2 201 8000 2024))
(def pr5 (cust/->PurchaseRecord 2 202 12000 2025))
(def pr6 (cust/->PurchaseRecord 3 301 5000 2025))
(def purchase-records [pr1 pr2 pr3 pr4 pr5 pr6])

;; Orders (created via ord/create-order)
;; create-order args: id cust-id line-specs products categories customers timestamp
;; line-specs: [[prod-id qty] ...]

;; Order 1: Alice buys Widget A x5 + Gadget B x2
(def order1 (ord/create-order 1001 1 [[1 5] [2 2]] products categories customers 1700000000))
;; Order 2: Bob buys Stapler x10 + Bolt Pack x20
(def order2 (ord/create-order 1002 2 [[3 10] [5 20]] products categories customers 1700100000))
;; Order 3: Alice buys Gear Set x1 + Desk Lamp x3
(def order3 (ord/create-order 1003 1 [[6 1] [4 3]] products categories customers 1700200000))
;; Order 4: Carol buys Widget A x2
(def order4 (ord/create-order 1004 3 [[1 2]] products categories customers 1700300000))
(def orders [order1 order2 order3 order4])

;; ---------------------------------------------------------------------------
;; Pre-calculate order values for assertions
;; ---------------------------------------------------------------------------

;; Order 1: Alice (gold, 15% discount)
;; lines: Widget A x5 = 5*1200 = 6000, Gadget B x2 = 2*1500 = 3000
;; subtotal = 9000
;; discount = 9000*15/100 = 1350
;; tax = long((9000-1350)*0.1) = long(765.0) = 765
;; total = 9000 + 765 - 1350 = 8415

;; Order 2: Bob (silver, 10% discount)
;; lines: Stapler x10 = 10*450 = 4500, Bolt Pack x20 = 20*120 = 2400
;; subtotal = 6900
;; discount = 6900*10/100 = 690
;; tax = long((6900-690)*0.1) = long(621.0) = 621
;; total = 6900 + 621 - 690 = 6831

;; Order 3: Alice (gold, 15% discount)
;; lines: Gear Set x1 = 1*2200 = 2200, Desk Lamp x3 = 3*750 = 2250
;; subtotal = 4450
;; discount = 4450*15/100 = 667 (integer division: quot)
;; tax = long((4450-667)*0.1) = long(378.3) = 378
;; total = 4450 + 378 - 667 = 4161

;; Order 4: Carol (bronze, 5% discount)
;; lines: Widget A x2 = 2*1200 = 2400
;; subtotal = 2400
;; discount = 2400*5/100 = 120
;; tax = long((2400-120)*0.1) = long(228.0) = 228
;; total = 2400 + 228 - 120 = 2508


;; ===========================================================================
;; CATALOG TESTS
;; ===========================================================================

(println "--- CATALOG ---")

;; product-margin
(assert-eq "cat/product-margin Widget A" 700 (cat/product-margin prod1))
(assert-eq "cat/product-margin Gadget B" 700 (cat/product-margin prod2))
(assert-eq "cat/product-margin Stapler" 250 (cat/product-margin prod3))
(assert-eq "cat/product-margin Desk Lamp" 400 (cat/product-margin prod4))
(assert-eq "cat/product-margin Bolt Pack" 70 (cat/product-margin prod5))
(assert-eq "cat/product-margin Gear Set" 1000 (cat/product-margin prod6))
(assert-eq "cat/product-margin Old Widget" 300 (cat/product-margin prod7))

;; product-margin-pct: (margin*100) / price, integer division
;; Widget A: 700*100/1200 = 58
;; Gadget B: 700*100/1500 = 46
;; Stapler: 250*100/450 = 55
;; Desk Lamp: 400*100/750 = 53
;; Bolt Pack: 70*100/120 = 58
;; Gear Set: 1000*100/2200 = 45
;; Old Widget: 300*100/600 = 50
(assert-eq "cat/product-margin-pct Widget A" 58 (cat/product-margin-pct prod1))
(assert-eq "cat/product-margin-pct Gadget B" 46 (cat/product-margin-pct prod2))
(assert-eq "cat/product-margin-pct Stapler" 55 (cat/product-margin-pct prod3))
(assert-eq "cat/product-margin-pct Desk Lamp" 53 (cat/product-margin-pct prod4))
(assert-eq "cat/product-margin-pct Bolt Pack" 58 (cat/product-margin-pct prod5))
(assert-eq "cat/product-margin-pct Gear Set" 45 (cat/product-margin-pct prod6))
(assert-eq "cat/product-margin-pct Old Widget" 50 (cat/product-margin-pct prod7))

;; product-profitable?
(assert-true "cat/product-profitable? Widget A" (cat/product-profitable? prod1))
(assert-true "cat/product-profitable? Gadget B" (cat/product-profitable? prod2))
(assert-true "cat/product-profitable? Stapler" (cat/product-profitable? prod3))
(assert-true "cat/product-profitable? Desk Lamp" (cat/product-profitable? prod4))
(assert-true "cat/product-profitable? Bolt Pack" (cat/product-profitable? prod5))
(assert-true "cat/product-profitable? Gear Set" (cat/product-profitable? prod6))
(assert-true "cat/product-profitable? Old Widget" (cat/product-profitable? prod7))

;; active-products: 6 active (all except Old Widget)
(assert-eq "cat/active-products count" 6 (count (cat/active-products products)))

;; inactive-products: 1 (Old Widget)
(assert-eq "cat/inactive-products count" 1 (count (cat/inactive-products products)))

;; products-by-category
;; cat 1 (Electronics): prod1, prod2, prod7 = 3
;; cat 2 (Office): prod3, prod4 = 2
;; cat 3 (Industrial): prod5, prod6 = 2
(assert-eq "cat/products-by-category cat 1" 3 (count (cat/products-by-category products 1)))
(assert-eq "cat/products-by-category cat 2" 2 (count (cat/products-by-category products 2)))
(assert-eq "cat/products-by-category cat 3" 2 (count (cat/products-by-category products 3)))

;; products-by-supplier
;; sup 1: prod1, prod2, prod7 = 3
;; sup 2: prod3, prod4 = 2
;; sup 3: prod5, prod6 = 2
(assert-eq "cat/products-by-supplier sup 1" 3 (count (cat/products-by-supplier products 1)))
(assert-eq "cat/products-by-supplier sup 2" 2 (count (cat/products-by-supplier products 2)))
(assert-eq "cat/products-by-supplier sup 3" 2 (count (cat/products-by-supplier products 3)))

;; find-product-by-id
(assert-eq "cat/find-product-by-id 1 name" "Widget A" (cat/product-name (cat/find-product-by-id products 1)))
(assert-eq "cat/find-product-by-id 3 name" "Stapler" (cat/product-name (cat/find-product-by-id products 3)))
(assert-eq "cat/find-product-by-id 99" nil (cat/find-product-by-id products 99))

;; find-product-by-sku
(assert-eq "cat/find-product-by-sku WGT-001" "Widget A" (cat/product-name (cat/find-product-by-sku products "WGT-001")))
(assert-eq "cat/find-product-by-sku GRS-006" "Gear Set" (cat/product-name (cat/find-product-by-sku products "GRS-006")))
(assert-eq "cat/find-product-by-sku NOPE" nil (cat/find-product-by-sku products "NOPE"))

;; sort-by-price: ascending by unit-price
;; 120, 450, 600, 750, 1200, 1500, 2200
(assert-eq "cat/sort-by-price first" "Bolt Pack" (cat/product-name (first (cat/sort-by-price products))))
(assert-eq "cat/sort-by-price last" "Gear Set" (cat/product-name (last (cat/sort-by-price products))))

;; total-catalog-value: sum of all unit-prices
;; 1200+1500+450+750+120+2200+600 = 6820
(assert-eq "cat/total-catalog-value" 6820 (cat/total-catalog-value products))

;; total-catalog-cost: sum of all unit-costs
;; 500+800+200+350+50+1200+300 = 3400
(assert-eq "cat/total-catalog-cost" 3400 (cat/total-catalog-cost products))

;; avg-unit-price: 6820/7 = 974 (integer division)
(assert-eq "cat/avg-unit-price" 974 (cat/avg-unit-price products))

;; avg-unit-cost: 3400/7 = 485
(assert-eq "cat/avg-unit-cost" 485 (cat/avg-unit-cost products))

;; cheapest-product (by cost): Bolt Pack (cost 50)
(assert-eq "cat/cheapest-product" "Bolt Pack" (cat/product-name (cat/cheapest-product products)))

;; most-expensive-product (by price): Gear Set (price 2200)
(assert-eq "cat/most-expensive-product" "Gear Set" (cat/product-name (cat/most-expensive-product products)))

;; category-product-count
(assert-eq "cat/category-product-count cat 1" 3 (cat/category-product-count products 1))
(assert-eq "cat/category-product-count cat 2" 2 (cat/category-product-count products 2))
(assert-eq "cat/category-product-count cat 3" 2 (cat/category-product-count products 3))

;; format-price
(assert-eq "cat/format-price 1200" "$12.00" (cat/format-price 1200))
(assert-eq "cat/format-price 50" "$0.50" (cat/format-price 50))
(assert-eq "cat/format-price 999" "$9.99" (cat/format-price 999))
(assert-eq "cat/format-price 0" "$0.00" (cat/format-price 0))
(assert-eq "cat/format-price 105" "$1.05" (cat/format-price 105))

;; product-tax: long(price * rate)
;; Widget A with Electronics (0.15): long(1200 * 0.15) = 180
;; Stapler with Office (0.08): long(450 * 0.08) = 36
;; Gear Set with Industrial (0.12): long(2200 * 0.12) = 264
(assert-eq "cat/product-tax Widget A Electronics" 180 (cat/product-tax prod1 cat1))
(assert-eq "cat/product-tax Stapler Office" 36 (cat/product-tax prod3 cat2))
(assert-eq "cat/product-tax Gear Set Industrial" 264 (cat/product-tax prod6 cat3))

;; high-margin-products: min-pct 50 -> products with margin-pct >= 50
;; Widget A=58, Gadget B=46, Stapler=55, Desk Lamp=53, Bolt Pack=58, Gear Set=45, Old Widget=50
;; >= 50: Widget A(58), Stapler(55), Desk Lamp(53), Bolt Pack(58), Old Widget(50) = 5
(assert-eq "cat/high-margin-products min 50 count" 5 (count (cat/high-margin-products products 50)))
;; >= 55: Widget A(58), Stapler(55), Bolt Pack(58) = 3
(assert-eq "cat/high-margin-products min 55 count" 3 (count (cat/high-margin-products products 55)))

;; deactivate-product: deactivating id 1 -> active count drops from 6 to 5
(let [deactivated (cat/deactivate-product products 1)]
  (assert-eq "cat/deactivate-product active count" 5 (count (cat/active-products deactivated))))

;; apply-price-increase: 10% on all products
;; Widget A price 1200 -> 1200 + 1200*10/100 = 1200 + 120 = 1320
(let [increased (cat/apply-price-increase products 10)]
  (assert-eq "cat/apply-price-increase Widget A" 1320 (cat/product-unit-price (first increased)))
  (assert-eq "cat/apply-price-increase Bolt Pack" 132 (cat/product-unit-price (nth increased 4))))

;; quantity-price: standard tiers [1=0%, 10=5%, 50=10%, 100=15%, 500=20%]
;; Widget A (price 1200), qty 100 -> tier is 100 (15% disc)
;; unit = 1200 - quot(1200*15/100) = 1200 - 180 = 1020
;; total = 100 * 1020 = 102000
(let [tiers (cat/make-standard-tiers)]
  (assert-eq "cat/quantity-price Widget A qty 100" 102000 (cat/quantity-price prod1 tiers 100))
  ;; qty 1 -> tier is 1 (0% disc) -> 1*1200 = 1200
  (assert-eq "cat/quantity-price Widget A qty 1" 1200 (cat/quantity-price prod1 tiers 1))
  ;; qty 10 -> tier is 10 (5% disc) -> 10*(1200 - 60) = 10*1140 = 11400
  (assert-eq "cat/quantity-price Widget A qty 10" 11400 (cat/quantity-price prod1 tiers 10))
  ;; qty 50 -> tier is 50 (10% disc) -> 50*(1200 - 120) = 50*1080 = 54000
  (assert-eq "cat/quantity-price Widget A qty 50" 54000 (cat/quantity-price prod1 tiers 50))
  ;; quantity-unit-price Widget A qty 100 -> 1020
  (assert-eq "cat/quantity-unit-price Widget A qty 100" 1020 (cat/quantity-unit-price prod1 tiers 100))
  ;; quantity-discount Widget A qty 100: full=120000, discounted=102000, discount=18000
  (assert-eq "cat/quantity-discount Widget A qty 100" 18000 (cat/quantity-discount prod1 tiers 100)))

;; product-summary
(assert-eq "cat/product-summary Widget A" "Widget A (WGT-001) $12.00" (cat/product-summary prod1))

;; valid-product?
(assert-true "cat/valid-product? prod1" (cat/valid-product? prod1))

;; products-in-price-range: 400 to 800 -> Stapler(450), Old Widget(600), Desk Lamp(750) = 3
(assert-eq "cat/products-in-price-range 400-800" 3 (count (cat/products-in-price-range products 400 800)))

;; top-n-by-price: top 2 -> Gear Set(2200), Gadget B(1500)
(let [top2 (cat/top-n-by-price products 2)]
  (assert-eq "cat/top-n-by-price 2 first" "Gear Set" (cat/product-name (first top2)))
  (assert-eq "cat/top-n-by-price 2 second" "Gadget B" (cat/product-name (second top2))))

;; category-total-value: cat 1 = 1200+1500+600 = 3300
(assert-eq "cat/category-total-value cat 1" 3300 (cat/category-total-value products 1))

;; supplier-product-count
(assert-eq "cat/supplier-product-count sup 1" 3 (cat/supplier-product-count products 1))

;; supplier-avg-lead-time: (7+14+3)/3 = 8
(assert-eq "cat/supplier-avg-lead-time" 8 (cat/supplier-avg-lead-time suppliers))

;; price-spread: max(2200) - min(120) = 2080
(assert-eq "cat/price-spread" 2080 (cat/price-spread products))

;; cost-spread: max(1200) - min(50) = 1150
(assert-eq "cat/cost-spread" 1150 (cat/cost-spread products))

;; cheaper-product
(assert-eq "cat/cheaper-product prod1 vs prod5" "Bolt Pack" (cat/product-name (cat/cheaper-product prod1 prod5)))

;; higher-margin-product (by pct): Widget A=58, Gadget B=46 -> Widget A
(assert-eq "cat/higher-margin-product prod1 vs prod2" "Widget A" (cat/product-name (cat/higher-margin-product prod1 prod2)))

;; product-price-diff: prod1(1200) - prod5(120) = 1080
(assert-eq "cat/product-price-diff prod1 prod5" 1080 (cat/product-price-diff prod1 prod5))


;; ===========================================================================
;; CUSTOMERS TESTS
;; ===========================================================================

(println "--- CUSTOMERS ---")

;; tier-discount-pct
(assert-eq "cust/tier-discount-pct gold" 15 (cust/tier-discount-pct "gold"))
(assert-eq "cust/tier-discount-pct silver" 10 (cust/tier-discount-pct "silver"))
(assert-eq "cust/tier-discount-pct bronze" 5 (cust/tier-discount-pct "bronze"))
(assert-eq "cust/tier-discount-pct none" 0 (cust/tier-discount-pct "none"))

;; tier-rank
(assert-eq "cust/tier-rank gold" 3 (cust/tier-rank "gold"))
(assert-eq "cust/tier-rank silver" 2 (cust/tier-rank "silver"))
(assert-eq "cust/tier-rank bronze" 1 (cust/tier-rank "bronze"))
(assert-eq "cust/tier-rank other" 0 (cust/tier-rank "other"))

;; higher-tier?
(assert-true "cust/higher-tier? gold vs silver" (cust/higher-tier? "gold" "silver"))
(assert-true "cust/higher-tier? gold vs bronze" (cust/higher-tier? "gold" "bronze"))
(assert-true "cust/higher-tier? silver vs bronze" (cust/higher-tier? "silver" "bronze"))
(assert-false "cust/higher-tier? bronze vs gold" (cust/higher-tier? "bronze" "gold"))
(assert-false "cust/higher-tier? silver vs gold" (cust/higher-tier? "silver" "gold"))
(assert-false "cust/higher-tier? gold vs gold" (cust/higher-tier? "gold" "gold"))

;; tier-from-spend
(assert-eq "cust/tier-from-spend 100000" "gold" (cust/tier-from-spend 100000))
(assert-eq "cust/tier-from-spend 150000" "gold" (cust/tier-from-spend 150000))
(assert-eq "cust/tier-from-spend 50000" "silver" (cust/tier-from-spend 50000))
(assert-eq "cust/tier-from-spend 75000" "silver" (cust/tier-from-spend 75000))
(assert-eq "cust/tier-from-spend 10000" "bronze" (cust/tier-from-spend 10000))
(assert-eq "cust/tier-from-spend 25000" "bronze" (cust/tier-from-spend 25000))
(assert-eq "cust/tier-from-spend 5000" "none" (cust/tier-from-spend 5000))
(assert-eq "cust/tier-from-spend 0" "none" (cust/tier-from-spend 0))

;; find-customer-by-id
(assert-eq "cust/find-customer-by-id 1" "Alice Smith" (cust/customer-name (cust/find-customer-by-id customers 1)))
(assert-eq "cust/find-customer-by-id 2" "Bob Jones" (cust/customer-name (cust/find-customer-by-id customers 2)))
(assert-eq "cust/find-customer-by-id 99" nil (cust/find-customer-by-id customers 99))

;; find-customer-by-email
(assert-eq "cust/find-customer-by-email alice" "Alice Smith" (cust/customer-name (cust/find-customer-by-email customers "alice@example.com")))
(assert-eq "cust/find-customer-by-email bob" "Bob Jones" (cust/customer-name (cust/find-customer-by-email customers "bob@example.com")))
(assert-eq "cust/find-customer-by-email unknown" nil (cust/find-customer-by-email customers "unknown@example.com"))

;; customers-by-tier
(assert-eq "cust/customers-by-tier gold count" 1 (count (cust/customers-by-tier customers "gold")))
(assert-eq "cust/customers-by-tier silver count" 1 (count (cust/customers-by-tier customers "silver")))
(assert-eq "cust/customers-by-tier bronze count" 1 (count (cust/customers-by-tier customers "bronze")))

;; customer-total-spend: Alice = 15000+22000+18000 = 55000
(assert-eq "cust/customer-total-spend Alice" 55000 (cust/customer-total-spend purchase-records 1))
;; Bob = 8000+12000 = 20000
(assert-eq "cust/customer-total-spend Bob" 20000 (cust/customer-total-spend purchase-records 2))
;; Carol = 5000
(assert-eq "cust/customer-total-spend Carol" 5000 (cust/customer-total-spend purchase-records 3))

;; customer-purchase-count
(assert-eq "cust/customer-purchase-count Alice" 3 (cust/customer-purchase-count purchase-records 1))
(assert-eq "cust/customer-purchase-count Bob" 2 (cust/customer-purchase-count purchase-records 2))
(assert-eq "cust/customer-purchase-count Carol" 1 (cust/customer-purchase-count purchase-records 3))

;; customer-avg-order-value: Alice = 55000/3 = 18333
(assert-eq "cust/customer-avg-order-value Alice" 18333 (cust/customer-avg-order-value purchase-records 1))
;; Bob = 20000/2 = 10000
(assert-eq "cust/customer-avg-order-value Bob" 10000 (cust/customer-avg-order-value purchase-records 2))
;; Carol = 5000/1 = 5000
(assert-eq "cust/customer-avg-order-value Carol" 5000 (cust/customer-avg-order-value purchase-records 3))

;; customer-spend-in-year
;; Alice 2024 = 15000+22000 = 37000
(assert-eq "cust/customer-spend-in-year Alice 2024" 37000 (cust/customer-spend-in-year purchase-records 1 2024))
;; Alice 2025 = 18000
(assert-eq "cust/customer-spend-in-year Alice 2025" 18000 (cust/customer-spend-in-year purchase-records 1 2025))
;; Bob 2024 = 8000
(assert-eq "cust/customer-spend-in-year Bob 2024" 8000 (cust/customer-spend-in-year purchase-records 2 2024))
;; Bob 2025 = 12000
(assert-eq "cust/customer-spend-in-year Bob 2025" 12000 (cust/customer-spend-in-year purchase-records 2 2025))
;; Carol 2025 = 5000
(assert-eq "cust/customer-spend-in-year Carol 2025" 5000 (cust/customer-spend-in-year purchase-records 3 2025))
;; Carol 2024 = 0
(assert-eq "cust/customer-spend-in-year Carol 2024" 0 (cust/customer-spend-in-year purchase-records 3 2024))

;; assess-tier: based on total spend via tier-from-spend
;; Alice 55000 -> silver (>= 50000)
(assert-eq "cust/assess-tier Alice" "silver" (cust/assess-tier purchase-records 1))
;; Bob 20000 -> bronze (>= 10000)
(assert-eq "cust/assess-tier Bob" "bronze" (cust/assess-tier purchase-records 2))
;; Carol 5000 -> none (< 10000)
(assert-eq "cust/assess-tier Carol" "none" (cust/assess-tier purchase-records 3))

;; customer-summary: contains name and tier
(let [summary (cust/customer-summary cust1)]
  (assert-true "cust/customer-summary contains Alice Smith" (.contains summary "Alice Smith"))
  (assert-true "cust/customer-summary contains gold" (.contains summary "gold")))

;; points-for-purchase: base = amount/100, multiplied by tier
;; 15000 with gold: 150 * 3 = 450
(assert-eq "cust/points-for-purchase 15000 gold" 450 (cust/points-for-purchase 15000 "gold"))
;; 10000 with silver: 100 * 2 = 200
(assert-eq "cust/points-for-purchase 10000 silver" 200 (cust/points-for-purchase 10000 "silver"))
;; 8000 with bronze: 80 * 1 = 80
(assert-eq "cust/points-for-purchase 8000 bronze" 80 (cust/points-for-purchase 8000 "bronze"))
;; 5000 with none: 50 * 1 = 50
(assert-eq "cust/points-for-purchase 5000 none" 50 (cust/points-for-purchase 5000 "none"))

;; customer-segment: based on spend and count
;; Alice: spend=55000, count=3 -> (>= 50000 AND >= 5) => false for regular, (>= count 1) => "occasional"
;; Actually: (and (>= 55000 100000) ...) = false, (and (>= 55000 50000) (>= 3 5)) = false, (>= 3 1) = true -> "occasional"
(assert-eq "cust/customer-segment Alice" "occasional" (cust/customer-segment purchase-records 1))
;; Bob: spend=20000, count=2 -> not vip, not regular, (>= 2 1) -> "occasional"
(assert-eq "cust/customer-segment Bob" "occasional" (cust/customer-segment purchase-records 2))
;; Carol: spend=5000, count=1 -> (>= 1 1) -> "occasional"
(assert-eq "cust/customer-segment Carol" "occasional" (cust/customer-segment purchase-records 3))

;; count-by-tier
(assert-eq "cust/count-by-tier" {:gold 1 :silver 1 :bronze 1} (cust/count-by-tier customers))

;; total-customer-count
(assert-eq "cust/total-customer-count" 3 (cust/total-customer-count customers))

;; customer-tenure (current year 2025)
(assert-eq "cust/customer-tenure Alice 2025" 5 (cust/customer-tenure cust1 2025))
(assert-eq "cust/customer-tenure Bob 2025" 4 (cust/customer-tenure cust2 2025))
(assert-eq "cust/customer-tenure Carol 2025" 2 (cust/customer-tenure cust3 2025))

;; avg-customer-tenure: (5+4+2)/3 = 3
(assert-eq "cust/avg-customer-tenure 2025" 3 (cust/avg-customer-tenure customers 2025))

;; valid-customer?
(assert-true "cust/valid-customer? cust1" (cust/valid-customer? cust1))
(assert-true "cust/valid-customer? cust2" (cust/valid-customer? cust2))

;; valid-address?
(assert-true "cust/valid-address? addr1" (cust/valid-address? addr1))

;; address-oneline
(assert-eq "cust/address-oneline addr1" "123 Main St, Chicago, IL 60601" (cust/address-oneline addr1))
(assert-eq "cust/address-oneline addr2" "456 Oak Ave, Portland, OR 97201" (cust/address-oneline addr2))

;; customers-by-state
(assert-eq "cust/customers-by-state IL" 1 (count (cust/customers-by-state customers "IL")))
(assert-eq "cust/customers-by-state OR" 1 (count (cust/customers-by-state customers "OR")))
(assert-eq "cust/customers-by-state TX" 1 (count (cust/customers-by-state customers "TX")))
(assert-eq "cust/customers-by-state CA" 0 (count (cust/customers-by-state customers "CA")))

;; unique-states: IL, OR, TX = 3
(assert-eq "cust/unique-states count" 3 (count (cust/unique-states customers)))

;; customers-since-year: 2022 -> Bob(2021? no), Carol(2023) = 1; 2020 -> all 3
(assert-eq "cust/customers-since-year 2023" 1 (count (cust/customers-since-year customers 2023)))
(assert-eq "cust/customers-since-year 2020" 3 (count (cust/customers-since-year customers 2020)))
(assert-eq "cust/customers-since-year 2021" 2 (count (cust/customers-since-year customers 2021)))

;; update-customer-email
(let [updated (cust/update-customer-email cust1 "newalice@example.com")]
  (assert-eq "cust/update-customer-email" "newalice@example.com" (cust/customer-email updated))
  (assert-eq "cust/update-customer-email preserves name" "Alice Smith" (cust/customer-name updated)))

;; points-to-dollars: 450 / 10 = 45
(assert-eq "cust/points-to-dollars 450" 45 (cust/points-to-dollars 450))
(assert-eq "cust/points-to-dollars 100" 10 (cust/points-to-dollars 100))


;; ===========================================================================
;; INVENTORY TESTS
;; ===========================================================================

(println "--- INVENTORY ---")

;; find-warehouse-by-id
(assert-eq "inv/find-warehouse-by-id 1" "Main DC" (inv/warehouse-name (inv/find-warehouse-by-id warehouses 1)))
(assert-eq "inv/find-warehouse-by-id 2" "West Hub" (inv/warehouse-name (inv/find-warehouse-by-id warehouses 2)))
(assert-eq "inv/find-warehouse-by-id 99" nil (inv/find-warehouse-by-id warehouses 99))

;; total-stock-for-product
;; prod 1: wh1=100 + wh2=75 = 175
(assert-eq "inv/total-stock-for-product prod 1" 175 (inv/total-stock-for-product stock-levels 1))
;; prod 2: wh1=50 + wh2=25 = 75
(assert-eq "inv/total-stock-for-product prod 2" 75 (inv/total-stock-for-product stock-levels 2))
;; prod 3: wh1=200 + wh2=0 = 200
(assert-eq "inv/total-stock-for-product prod 3" 200 (inv/total-stock-for-product stock-levels 3))
;; prod 4: wh1=30 only = 30
(assert-eq "inv/total-stock-for-product prod 4" 30 (inv/total-stock-for-product stock-levels 4))
;; prod 5: wh1=500 + wh2=300 = 800
(assert-eq "inv/total-stock-for-product prod 5" 800 (inv/total-stock-for-product stock-levels 5))
;; prod 6: wh1=8 only = 8
(assert-eq "inv/total-stock-for-product prod 6" 8 (inv/total-stock-for-product stock-levels 6))

;; total-stock-in-warehouse
;; wh1: 100+50+200+30+500+8 = 888
(assert-eq "inv/total-stock-in-warehouse wh 1" 888 (inv/total-stock-in-warehouse stock-levels 1))
;; wh2: 75+25+0+300 = 400
(assert-eq "inv/total-stock-in-warehouse wh 2" 400 (inv/total-stock-in-warehouse stock-levels 2))

;; in-stock?
(assert-true "inv/in-stock? wh1 prod1" (inv/in-stock? stock-levels 1 1))
(assert-true "inv/in-stock? wh1 prod6" (inv/in-stock? stock-levels 1 6))
(assert-true "inv/in-stock? wh2 prod1" (inv/in-stock? stock-levels 2 1))
(assert-false "inv/in-stock? wh2 prod3 (qty=0)" (inv/in-stock? stock-levels 2 3))
;; prod 4 not in wh2 at all -> nil -> false
(assert-false "inv/in-stock? wh2 prod4 (no record)" (inv/in-stock? stock-levels 2 4))

;; below-minimum?
;; sl-1-6: qty=8, min=10 -> true
(assert-true "inv/below-minimum? sl-1-6" (inv/below-minimum? sl-1-6))
;; sl-2-3: qty=0, min=30 -> true
(assert-true "inv/below-minimum? sl-2-3" (inv/below-minimum? sl-2-3))
;; sl-1-1: qty=100, min=20 -> false
(assert-false "inv/below-minimum? sl-1-1" (inv/below-minimum? sl-1-1))
;; sl-1-2: qty=50, min=15 -> false
(assert-false "inv/below-minimum? sl-1-2" (inv/below-minimum? sl-1-2))

;; low-stock-items: items where qty < min
;; sl-1-6 (8<10), sl-2-3 (0<30) = 2
(assert-eq "inv/low-stock-items count" 2 (count (inv/low-stock-items stock-levels)))

;; out-of-stock-items: qty = 0
;; sl-2-3 only
(assert-eq "inv/out-of-stock-items count" 1 (count (inv/out-of-stock-items stock-levels)))

;; can-fulfill?
;; prod 1, qty 100: total=175, 175>=100 -> true
(assert-true "inv/can-fulfill? prod1 100" (inv/can-fulfill? stock-levels 1 100))
;; prod 1, qty 175: 175>=175 -> true
(assert-true "inv/can-fulfill? prod1 175" (inv/can-fulfill? stock-levels 1 175))
;; prod 1, qty 176: 175>=176 -> false
(assert-false "inv/can-fulfill? prod1 176" (inv/can-fulfill? stock-levels 1 176))
;; prod 3, qty 200: total=200, 200>=200 -> true
(assert-true "inv/can-fulfill? prod3 200" (inv/can-fulfill? stock-levels 3 200))
;; prod 3, qty 250: 200>=250 -> false
(assert-false "inv/can-fulfill? prod3 250" (inv/can-fulfill? stock-levels 3 250))
;; prod 6, qty 8: 8>=8 -> true
(assert-true "inv/can-fulfill? prod6 8" (inv/can-fulfill? stock-levels 6 8))
;; prod 6, qty 9: 8>=9 -> false
(assert-false "inv/can-fulfill? prod6 9" (inv/can-fulfill? stock-levels 6 9))

;; stock-value-at-warehouse: sum of (qty * product unit-cost) for warehouse
;; wh1: 100*500 + 50*800 + 200*200 + 30*350 + 500*50 + 8*1200
;;     = 50000 + 40000 + 40000 + 10500 + 25000 + 9600 = 175100
(assert-eq "inv/stock-value-at-warehouse wh1" 175100 (inv/stock-value-at-warehouse stock-levels 1 products))
;; wh2: 75*500 + 25*800 + 0*200 + 300*50
;;     = 37500 + 20000 + 0 + 15000 = 72500
(assert-eq "inv/stock-value-at-warehouse wh2" 72500 (inv/stock-value-at-warehouse stock-levels 2 products))

;; total-inventory-value: 175100 + 72500 = 247600
(assert-eq "inv/total-inventory-value" 247600 (inv/total-inventory-value stock-levels products))

;; stock-retail-value: sum of (qty * unit-price)
;; wh1: 100*1200 + 50*1500 + 200*450 + 30*750 + 500*120 + 8*2200
;;     = 120000 + 75000 + 90000 + 22500 + 60000 + 17600 = 385100
;; wh2: 75*1200 + 25*1500 + 0*450 + 300*120
;;     = 90000 + 37500 + 0 + 36000 = 163500
;; total = 385100 + 163500 = 548600
(assert-eq "inv/stock-retail-value" 548600 (inv/stock-retail-value stock-levels products))

;; reorder-needed: same as low-stock-items (qty < min)
(assert-eq "inv/reorder-needed count" 2 (count (inv/reorder-needed stock-levels)))

;; reorder-quantity: deficit * 2
;; sl-1-6: deficit = 10-8 = 2, reorder = 4
(assert-eq "inv/reorder-quantity sl-1-6" 4 (inv/reorder-quantity sl-1-6))
;; sl-2-3: deficit = 30-0 = 30, reorder = 60
(assert-eq "inv/reorder-quantity sl-2-3" 60 (inv/reorder-quantity sl-2-3))
;; sl-1-1: deficit = 20-100 = -80, not > 0 -> 0
(assert-eq "inv/reorder-quantity sl-1-1" 0 (inv/reorder-quantity sl-1-1))

;; reorder-cost: sum of (reorder-qty * product-cost) for items needing reorder
;; sl-1-6: 4 * 1200(Gear Set cost) = 4800
;; sl-2-3: 60 * 200(Stapler cost) = 12000
;; total = 16800
(assert-eq "inv/reorder-cost" 16800 (inv/reorder-cost stock-levels products))

;; warehouse-utilization-pct: total-units * 100 / capacity
;; wh1: 888 * 100 / 10000 = 8
(assert-eq "inv/warehouse-utilization-pct wh1" 8 (inv/warehouse-utilization-pct stock-levels wh1))
;; wh2: 400 * 100 / 5000 = 8
(assert-eq "inv/warehouse-utilization-pct wh2" 8 (inv/warehouse-utilization-pct stock-levels wh2))

;; warehouse-item-count: distinct stock level entries per warehouse
;; wh1: 6 entries
(assert-eq "inv/warehouse-item-count wh1" 6 (inv/warehouse-item-count stock-levels 1))
;; wh2: 4 entries
(assert-eq "inv/warehouse-item-count wh2" 4 (inv/warehouse-item-count stock-levels 2))

;; apply-movement: add 10 units to wh1/prod1
(let [mv (inv/->StockMovement 1 1 1 10 "receive" 1700000000)
      new-levels (inv/apply-movement stock-levels mv)
      updated-sl (inv/find-stock new-levels 1 1)]
  (assert-eq "inv/apply-movement receive +10" 110 (inv/stocklevel-quantity updated-sl)))

;; apply-movement: ship 20 from wh1/prod3
(let [mv (inv/->StockMovement 2 1 3 -20 "ship" 1700000000)
      new-levels (inv/apply-movement stock-levels mv)
      updated-sl (inv/find-stock new-levels 1 3)]
  (assert-eq "inv/apply-movement ship -20" 180 (inv/stocklevel-quantity updated-sl)))

;; can-transfer?
;; wh1 prod1 qty 50: has 100 >= 50 -> true
(assert-true "inv/can-transfer? wh1 prod1 50" (inv/can-transfer? stock-levels 1 1 50))
;; wh1 prod1 qty 200: has 100 >= 200 -> false
(assert-false "inv/can-transfer? wh1 prod1 200" (inv/can-transfer? stock-levels 1 1 200))
;; wh1 prod1 qty 100: has 100 >= 100 -> true
(assert-true "inv/can-transfer? wh1 prod1 100" (inv/can-transfer? stock-levels 1 1 100))
;; wh2 prod4: no stock level for prod4 in wh2 -> nil -> false
(assert-false "inv/can-transfer? wh2 prod4" (inv/can-transfer? stock-levels 2 4 1))

;; apply-transfer: transfer 20 of prod1 from wh1 to wh2
(let [tr (inv/create-transfer 1 1 2 1 20 1700000000)
      new-levels (inv/apply-transfer stock-levels tr)
      from-sl (inv/find-stock new-levels 1 1)
      to-sl (inv/find-stock new-levels 2 1)]
  (assert-eq "inv/apply-transfer from wh1 prod1" 80 (inv/stocklevel-quantity from-sl))
  (assert-eq "inv/apply-transfer to wh2 prod1" 95 (inv/stocklevel-quantity to-sl)))

;; stock-summary format
(assert-eq "inv/stock-summary sl-1-1" "WH:1 Prod:1 Qty:100/20" (inv/stock-summary sl-1-1))

;; warehouse-summary format
(assert-eq "inv/warehouse-summary wh1" "Main DC (Chicago) cap:10000" (inv/warehouse-summary wh1))

;; find-stock
(assert-eq "inv/find-stock wh1 prod2 qty" 50 (inv/stocklevel-quantity (inv/find-stock stock-levels 1 2)))
(assert-eq "inv/find-stock wh2 prod5 qty" 300 (inv/stocklevel-quantity (inv/find-stock stock-levels 2 5)))
(assert-eq "inv/find-stock wh2 prod4" nil (inv/find-stock stock-levels 2 4))

;; days-of-stock: total / daily-demand
;; prod1: total=175, demand=5 -> 35
(assert-eq "inv/days-of-stock prod1 demand 5" 35 (inv/days-of-stock stock-levels 1 5))
;; prod5: total=800, demand=10 -> 80
(assert-eq "inv/days-of-stock prod5 demand 10" 80 (inv/days-of-stock stock-levels 5 10))
;; zero demand -> 999
(assert-eq "inv/days-of-stock zero demand" 999 (inv/days-of-stock stock-levels 1 0))

;; safety-stock: avg-daily-demand * lead-time-days * safety-factor
(assert-eq "inv/safety-stock 10 7 2" 140 (inv/safety-stock 10 7 2))
(assert-eq "inv/safety-stock 5 14 1" 70 (inv/safety-stock 5 14 1))

;; stock-value-for-product
;; prod1: total-qty=175, cost=500 -> 87500
(assert-eq "inv/stock-value-for-product prod1" 87500 (inv/stock-value-for-product stock-levels 1 products))
;; prod6: total-qty=8, cost=1200 -> 9600
(assert-eq "inv/stock-value-for-product prod6" 9600 (inv/stock-value-for-product stock-levels 6 products))

;; batch-receive: add items to wh1
(let [new-levels (inv/batch-receive stock-levels 1 [[1 10] [2 5]])]
  (assert-eq "inv/batch-receive prod1 wh1" 110 (inv/stocklevel-quantity (inv/find-stock new-levels 1 1)))
  (assert-eq "inv/batch-receive prod2 wh1" 55 (inv/stocklevel-quantity (inv/find-stock new-levels 1 2))))

;; batch-ship: remove items from wh1
(let [new-levels (inv/batch-ship stock-levels 1 [[1 10] [3 50]])]
  (assert-eq "inv/batch-ship prod1 wh1" 90 (inv/stocklevel-quantity (inv/find-stock new-levels 1 1)))
  (assert-eq "inv/batch-ship prod3 wh1" 150 (inv/stocklevel-quantity (inv/find-stock new-levels 1 3))))


;; ===========================================================================
;; ORDERS TESTS
;; ===========================================================================

(println "--- ORDERS ---")

;; make-order-line
(let [ol (ord/make-order-line products 1 3)]
  (assert-eq "ord/make-order-line prod1 qty3 product-id" 1 (ord/orderline-product-id ol))
  (assert-eq "ord/make-order-line prod1 qty3 quantity" 3 (ord/orderline-quantity ol))
  (assert-eq "ord/make-order-line prod1 qty3 unit-price" 1200 (ord/orderline-unit-price ol))
  (assert-eq "ord/make-order-line prod1 qty3 line-total" 3600 (ord/orderline-line-total ol)))

(let [ol (ord/make-order-line products 5 20)]
  (assert-eq "ord/make-order-line prod5 qty20 line-total" 2400 (ord/orderline-line-total ol)))

;; calculate-subtotal
(let [lines [(ord/make-order-line products 1 2) (ord/make-order-line products 3 5)]]
  ;; 2*1200 + 5*450 = 2400 + 2250 = 4650
  (assert-eq "ord/calculate-subtotal" 4650 (ord/calculate-subtotal lines)))

;; calculate-tax: long(subtotal * rate)
(assert-eq "ord/calculate-tax 10000 0.10" 1000 (ord/calculate-tax 10000 0.10))
(assert-eq "ord/calculate-tax 7650 0.10" 765 (ord/calculate-tax 7650 0.10))

;; calculate-discount: subtotal * pct / 100
(assert-eq "ord/calculate-discount 10000 15" 1500 (ord/calculate-discount 10000 15))
(assert-eq "ord/calculate-discount 9000 10" 900 (ord/calculate-discount 9000 10))
(assert-eq "ord/calculate-discount 6900 10" 690 (ord/calculate-discount 6900 10))

;; calculate-total: subtotal + tax - discount
(assert-eq "ord/calculate-total 9000 765 1350" 8415 (ord/calculate-total 9000 765 1350))

;; customer-discount-pct
(assert-eq "ord/customer-discount-pct cust1 gold" 15 (ord/customer-discount-pct customers 1))
(assert-eq "ord/customer-discount-pct cust2 silver" 10 (ord/customer-discount-pct customers 2))
(assert-eq "ord/customer-discount-pct cust3 bronze" 5 (ord/customer-discount-pct customers 3))
(assert-eq "ord/customer-discount-pct unknown" 0 (ord/customer-discount-pct customers 99))

;; Verify order1 structure
;; Order 1: Alice gold 15% disc
;; lines: [1 5] -> 5*1200=6000, [2 2] -> 2*1500=3000
;; subtotal=9000, disc=1350, tax=long((9000-1350)*0.1)=765, total=9000+765-1350=8415
(assert-eq "order1 id" 1001 (ord/order-id order1))
(assert-eq "order1 customer-id" 1 (ord/order-customer-id order1))
(assert-eq "order1 status" "pending" (ord/order-status order1))
(assert-eq "order1 subtotal" 9000 (ord/order-subtotal order1))
(assert-eq "order1 discount" 1350 (ord/order-discount order1))
(assert-eq "order1 tax" 765 (ord/order-tax order1))
(assert-eq "order1 total" 8415 (ord/order-total order1))
(assert-eq "order1 line count" 2 (count (ord/order-lines order1)))

;; Verify order2 structure
;; Order 2: Bob silver 10% disc
;; lines: [3 10] -> 10*450=4500, [5 20] -> 20*120=2400
;; subtotal=6900, disc=690, tax=long((6900-690)*0.1)=621, total=6900+621-690=6831
(assert-eq "order2 subtotal" 6900 (ord/order-subtotal order2))
(assert-eq "order2 discount" 690 (ord/order-discount order2))
(assert-eq "order2 tax" 621 (ord/order-tax order2))
(assert-eq "order2 total" 6831 (ord/order-total order2))

;; Verify order3 structure
;; Order 3: Alice gold 15% disc
;; lines: [6 1] -> 1*2200=2200, [4 3] -> 3*750=2250
;; subtotal=4450, disc=quot(4450*15/100)=667, tax=long((4450-667)*0.1)=long(378.3)=378
;; total=4450+378-667=4161
(assert-eq "order3 subtotal" 4450 (ord/order-subtotal order3))
(assert-eq "order3 discount" 667 (ord/order-discount order3))
(assert-eq "order3 tax" 378 (ord/order-tax order3))
(assert-eq "order3 total" 4161 (ord/order-total order3))

;; Verify order4 structure
;; Order 4: Carol bronze 5% disc
;; lines: [1 2] -> 2*1200=2400
;; subtotal=2400, disc=120, tax=long((2400-120)*0.1)=long(228.0)=228
;; total=2400+228-120=2508
(assert-eq "order4 subtotal" 2400 (ord/order-subtotal order4))
(assert-eq "order4 discount" 120 (ord/order-discount order4))
(assert-eq "order4 tax" 228 (ord/order-tax order4))
(assert-eq "order4 total" 2508 (ord/order-total order4))

;; update-status / confirm
(let [confirmed (ord/confirm-order order1)]
  (assert-eq "ord/confirm-order status" "confirmed" (ord/order-status confirmed))
  (assert-eq "ord/confirm-order preserves total" 8415 (ord/order-total confirmed)))

;; order-pending?
(assert-true "ord/order-pending? order1" (ord/order-pending? order1))
(assert-false "ord/order-pending? confirmed" (ord/order-pending? (ord/confirm-order order1)))

;; order-active?
(assert-true "ord/order-active? pending" (ord/order-active? order1))
(assert-true "ord/order-active? confirmed" (ord/order-active? (ord/confirm-order order1)))
(assert-false "ord/order-active? cancelled" (ord/order-active? (ord/cancel-order order1)))
(assert-false "ord/order-active? delivered" (ord/order-active? (ord/deliver-order order1)))

;; can-fulfill-order?
;; order1: Widget A x5 (total=175, need 5 -> OK), Gadget B x2 (total=75, need 2 -> OK)
(assert-true "ord/can-fulfill-order? order1" (ord/can-fulfill-order? stock-levels order1))
;; order2: Stapler x10 (total=200, need 10 -> OK), Bolt Pack x20 (total=800, need 20 -> OK)
(assert-true "ord/can-fulfill-order? order2" (ord/can-fulfill-order? stock-levels order2))
;; order3: Gear Set x1 (total=8, need 1 -> OK), Desk Lamp x3 (total=30, need 3 -> OK)
(assert-true "ord/can-fulfill-order? order3" (ord/can-fulfill-order? stock-levels order3))

;; Test a big order that cannot be fulfilled
(let [big-order (ord/create-order 9999 1 [[1 200]] products categories customers 1700000000)]
  ;; Widget A total stock = 175, need 200 -> cannot fulfill
  (assert-false "ord/can-fulfill-order? big order" (ord/can-fulfill-order? stock-levels big-order)))

;; orders-by-customer
(assert-eq "ord/orders-by-customer Alice" 2 (count (ord/orders-by-customer orders 1)))
(assert-eq "ord/orders-by-customer Bob" 1 (count (ord/orders-by-customer orders 2)))
(assert-eq "ord/orders-by-customer Carol" 1 (count (ord/orders-by-customer orders 3)))

;; orders-by-status: all are pending
(assert-eq "ord/orders-by-status pending" 4 (count (ord/orders-by-status orders "pending")))
(assert-eq "ord/orders-by-status confirmed" 0 (count (ord/orders-by-status orders "confirmed")))

;; pending-orders
(assert-eq "ord/pending-orders count" 4 (count (ord/pending-orders orders)))

;; active-orders: all 4 are active (pending is active)
(assert-eq "ord/active-orders count" 4 (count (ord/active-orders orders)))

;; total-revenue: 8415 + 6831 + 4161 + 2508 = 21915
(assert-eq "ord/total-revenue" 21915 (ord/total-revenue orders))

;; avg-order-value: 21915/4 = 5478
(assert-eq "ord/avg-order-value" 5478 (ord/avg-order-value orders))

;; total-discounts-given: 1350 + 690 + 667 + 120 = 2827
(assert-eq "ord/total-discounts-given" 2827 (ord/total-discounts-given orders))

;; total-tax-collected: 765 + 621 + 378 + 228 = 1992
(assert-eq "ord/total-tax-collected" 1992 (ord/total-tax-collected orders))

;; product-units-ordered
;; prod 1 (Widget A): order1 x5 + order4 x2 = 7
(assert-eq "ord/product-units-ordered prod1" 7 (ord/product-units-ordered orders 1))
;; prod 2 (Gadget B): order1 x2 = 2
(assert-eq "ord/product-units-ordered prod2" 2 (ord/product-units-ordered orders 2))
;; prod 3 (Stapler): order2 x10 = 10
(assert-eq "ord/product-units-ordered prod3" 10 (ord/product-units-ordered orders 3))
;; prod 4 (Desk Lamp): order3 x3 = 3
(assert-eq "ord/product-units-ordered prod4" 3 (ord/product-units-ordered orders 4))
;; prod 5 (Bolt Pack): order2 x20 = 20
(assert-eq "ord/product-units-ordered prod5" 20 (ord/product-units-ordered orders 5))
;; prod 6 (Gear Set): order3 x1 = 1
(assert-eq "ord/product-units-ordered prod6" 1 (ord/product-units-ordered orders 6))

;; product-revenue
;; prod 1: order1(5*1200=6000) + order4(2*1200=2400) = 8400
(assert-eq "ord/product-revenue prod1" 8400 (ord/product-revenue orders 1))
;; prod 2: order1(2*1500=3000) = 3000
(assert-eq "ord/product-revenue prod2" 3000 (ord/product-revenue orders 2))
;; prod 3: order2(10*450=4500) = 4500
(assert-eq "ord/product-revenue prod3" 4500 (ord/product-revenue orders 3))
;; prod 5: order2(20*120=2400) = 2400
(assert-eq "ord/product-revenue prod5" 2400 (ord/product-revenue orders 5))
;; prod 6: order3(1*2200=2200) = 2200
(assert-eq "ord/product-revenue prod6" 2200 (ord/product-revenue orders 6))

;; total-units-ordered: 5+2 + 10+20 + 1+3 + 2 = 43
(assert-eq "ord/total-units-ordered" 43 (ord/total-units-ordered orders))

;; total-line-count: 2+2+2+1 = 7
(assert-eq "ord/total-line-count" 7 (ord/total-line-count orders))

;; largest-order: order1 (8415)
(assert-eq "ord/largest-order" 1001 (ord/order-id (ord/largest-order orders)))

;; smallest-order: order4 (2508)
(assert-eq "ord/smallest-order" 1004 (ord/order-id (ord/smallest-order orders)))

;; customer-order-count
(assert-eq "ord/customer-order-count Alice" 2 (ord/customer-order-count orders 1))
(assert-eq "ord/customer-order-count Bob" 1 (ord/customer-order-count orders 2))

;; customer-total-spend (order module version)
;; Alice: 8415 + 4161 = 12576
(assert-eq "ord/customer-total-spend Alice" 12576 (ord/customer-total-spend orders 1))
;; Bob: 6831
(assert-eq "ord/customer-total-spend Bob" 6831 (ord/customer-total-spend orders 2))
;; Carol: 2508
(assert-eq "ord/customer-total-spend Carol" 2508 (ord/customer-total-spend orders 3))

;; customer-avg-order: Alice = 12576/2 = 6288
(assert-eq "ord/customer-avg-order Alice" 6288 (ord/customer-avg-order orders 1))

;; create-return
(let [ret (ord/create-return 1 1001 1 2 "defective")]
  (assert-eq "ord/create-return status" "pending" (ord/returnrequest-status ret))
  (assert-eq "ord/create-return product-id" 1 (ord/returnrequest-product-id ret))
  (assert-eq "ord/create-return quantity" 2 (ord/returnrequest-quantity ret)))

;; approve-return
(let [ret (ord/create-return 1 1001 1 2 "defective")
      approved (ord/approve-return ret)]
  (assert-eq "ord/approve-return status" "approved" (ord/returnrequest-status approved))
  (assert-eq "ord/approve-return preserves qty" 2 (ord/returnrequest-quantity approved)))

;; deny-return
(let [ret (ord/create-return 1 1001 1 2 "defective")
      denied (ord/deny-return ret)]
  (assert-eq "ord/deny-return status" "denied" (ord/returnrequest-status denied)))

;; return-refund-amount: price * quantity
;; prod 1 (Widget A price=1200), qty 2 -> 2400
(let [ret (ord/create-return 1 1001 1 2 "defective")]
  (assert-eq "ord/return-refund-amount" 2400 (ord/return-refund-amount ret products)))

;; partial-fulfill-order: with order that has unfulfillable line
(let [big-order (ord/create-order 9998 1 [[1 200] [6 5]] products categories customers 1700000000)
      parts (ord/partial-fulfill-order stock-levels big-order)]
  ;; prod1: requested=200, avail=175, ship=175, backordered=25
  (assert-eq "ord/partial-fulfill prod1 shipping" 175 (:shipping (first parts)))
  (assert-eq "ord/partial-fulfill prod1 backordered" 25 (:backordered (first parts)))
  ;; prod6: requested=5, avail=8, ship=5, backordered=0
  (assert-eq "ord/partial-fulfill prod6 shipping" 5 (:shipping (second parts)))
  (assert-eq "ord/partial-fulfill prod6 backordered" 0 (:backordered (second parts))))

;; add-line-to-order: add Bolt Pack x10 to order1
(let [new-line (ord/make-order-line products 5 10)
      updated (ord/add-line-to-order order1 new-line)]
  ;; new subtotal: 9000 + 10*120 = 10200
  (assert-eq "ord/add-line-to-order subtotal" 10200 (ord/order-subtotal updated))
  (assert-eq "ord/add-line-to-order line count" 3 (count (ord/order-lines updated)))
  ;; discount stays at 1350 (original discount)
  (assert-eq "ord/add-line-to-order discount unchanged" 1350 (ord/order-discount updated))
  ;; tax = long((10200 - 1350) * 0.1) = long(885.0) = 885
  (assert-eq "ord/add-line-to-order tax" 885 (ord/order-tax updated))
  ;; total = 10200 + 885 - 1350 = 9735
  (assert-eq "ord/add-line-to-order total" 9735 (ord/order-total updated)))

;; valid-order?
(assert-true "ord/valid-order? order1" (ord/valid-order? order1))
(assert-true "ord/valid-order? order2" (ord/valid-order? order2))

;; find-order-by-id
(assert-eq "ord/find-order-by-id 1001" 1001 (ord/order-id (ord/find-order-by-id orders 1001)))
(assert-eq "ord/find-order-by-id 1004" 1004 (ord/order-id (ord/find-order-by-id orders 1004)))
(assert-eq "ord/find-order-by-id 9999" nil (ord/find-order-by-id orders 9999))

;; orders-in-period
(assert-eq "ord/orders-in-period all" 4 (count (ord/orders-in-period orders 1700000000 1700300000)))
(assert-eq "ord/orders-in-period first two" 2 (count (ord/orders-in-period orders 1700000000 1700100000)))

;; revenue-in-period
;; orders 1+2: 8415+6831 = 15246
(assert-eq "ord/revenue-in-period first two" 15246 (ord/revenue-in-period orders 1700000000 1700100000))

;; average-items-per-order: 7 lines / 4 orders = 1 (integer division)
(assert-eq "ord/average-items-per-order" 1 (ord/average-items-per-order orders))

;; order-product-ids
(assert-eq "ord/order-product-ids order1" [1 2] (ord/order-product-ids order1))
(assert-eq "ord/order-product-ids order4" [1] (ord/order-product-ids order4))


;; ===========================================================================
;; REPORTS TESTS
;; ===========================================================================

(println "--- REPORTS ---")

;; sales-summary
(let [ss (rep/sales-summary orders)]
  (assert-eq "rep/sales-summary total-orders" 4 (:total-orders ss))
  (assert-eq "rep/sales-summary active-orders" 4 (:active-orders ss))
  (assert-eq "rep/sales-summary pending-orders" 4 (:pending-orders ss))
  (assert-eq "rep/sales-summary total-revenue" 21915 (:total-revenue ss))
  (assert-eq "rep/sales-summary total-tax" 1992 (:total-tax ss))
  (assert-eq "rep/sales-summary total-discounts" 2827 (:total-discounts ss))
  (assert-eq "rep/sales-summary avg-order-value" 5478 (:avg-order-value ss)))

;; fulfillment-rate: all 4 pending orders are fulfillable -> 100%
(assert-eq "rep/fulfillment-rate all fulfillable" 100 (rep/fulfillment-rate orders stock-levels))

;; Test with unfulfillable order included
(let [big-order (ord/create-order 9997 1 [[1 200]] products categories customers 1700000000)
      all-orders (conj orders big-order)]
  ;; 5 pending orders, 4 fulfillable, 1 not -> 4*100/5 = 80
  (assert-eq "rep/fulfillment-rate with unfulfillable" 80 (rep/fulfillment-rate all-orders stock-levels)))

;; fulfillment-report
(let [fr (rep/fulfillment-report orders stock-levels)]
  (assert-eq "rep/fulfillment-report count" 4 (count fr))
  (assert-true "rep/fulfillment-report order1 can-fulfill" (:can-fulfill (first fr))))

;; dashboard
(let [dash (rep/dashboard products suppliers categories stock-levels warehouses customers orders)]
  ;; catalog section
  (assert-eq "rep/dashboard catalog total-products" 7 (get-in dash [:catalog :total-products]))
  (assert-eq "rep/dashboard catalog active-products" 6 (get-in dash [:catalog :active-products]))
  (assert-eq "rep/dashboard catalog total-value" 6820 (get-in dash [:catalog :total-value]))
  (assert-eq "rep/dashboard catalog avg-price" 974 (get-in dash [:catalog :avg-price]))
  ;; inventory section
  (assert-eq "rep/dashboard inventory total-value" 247600 (get-in dash [:inventory :total-value]))
  (assert-eq "rep/dashboard inventory retail-value" 548600 (get-in dash [:inventory :retail-value]))
  (assert-eq "rep/dashboard inventory low-stock-count" 2 (get-in dash [:inventory :low-stock-count]))
  (assert-eq "rep/dashboard inventory reorder-cost" 16800 (get-in dash [:inventory :reorder-cost]))
  ;; customers section
  (assert-eq "rep/dashboard customers total-count" 3 (get-in dash [:customers :total-count]))
  (assert-eq "rep/dashboard customers tier-counts" {:gold 1 :silver 1 :bronze 1} (get-in dash [:customers :tier-counts]))
  ;; orders section
  (assert-eq "rep/dashboard orders total-orders" 4 (get-in dash [:orders :total-orders]))
  (assert-eq "rep/dashboard orders total-revenue" 21915 (get-in dash [:orders :total-revenue]))
  (assert-eq "rep/dashboard orders avg-order-value" 5478 (get-in dash [:orders :avg-order-value]))
  (assert-eq "rep/dashboard orders fulfillment-rate" 100 (get-in dash [:orders :fulfillment-rate])))

;; product-profitability
;; revenue - COGS for each active product
;; prod1: revenue=8400, units=7, cogs=7*500=3500, profit=4900
;; prod2: revenue=3000, units=2, cogs=2*800=1600, profit=1400
;; prod3: revenue=4500, units=10, cogs=10*200=2000, profit=2500
;; prod4: revenue=2250 (3*750), units=3, cogs=3*350=1050, profit=1200
;; prod5: revenue=2400, units=20, cogs=20*50=1000, profit=1400
;; prod6: revenue=2200, units=1, cogs=1*1200=1200, profit=1000
(let [prof (rep/product-profitability orders products)
      by-name (into {} (mapv (fn [r] [(:product-name r) r]) prof))]
  (assert-eq "rep/product-profitability Widget A revenue" 8400 (:revenue (get by-name "Widget A")))
  (assert-eq "rep/product-profitability Widget A cogs" 3500 (:cost-of-goods (get by-name "Widget A")))
  (assert-eq "rep/product-profitability Widget A profit" 4900 (:gross-profit (get by-name "Widget A")))
  (assert-eq "rep/product-profitability Stapler profit" 2500 (:gross-profit (get by-name "Stapler")))
  (assert-eq "rep/product-profitability Gear Set profit" 1000 (:gross-profit (get by-name "Gear Set")))
  ;; margin-pct: Widget A = 4900*100/8400 = 58
  (assert-eq "rep/product-profitability Widget A margin" 58 (:margin-pct (get by-name "Widget A"))))

;; period-comparison
;; period1: orders in [1700000000, 1700100000] = order1(8415) + order2(6831) = 15246, count=2
;; period2: orders in [1700200000, 1700300000] = order3(4161) + order4(2508) = 6669, count=2
;; change = 6669 - 15246 = -8577
;; change_pct = -8577 * 100 / 15246 = -56 (integer division)
(let [pc (rep/period-comparison orders 1700000000 1700100000 1700200000 1700300000)]
  (assert-eq "rep/period-comparison p1-revenue" 15246 (:period1-revenue pc))
  (assert-eq "rep/period-comparison p1-orders" 2 (:period1-orders pc))
  (assert-eq "rep/period-comparison p2-revenue" 6669 (:period2-revenue pc))
  (assert-eq "rep/period-comparison p2-orders" 2 (:period2-orders pc))
  (assert-eq "rep/period-comparison revenue-change" -8577 (:revenue-change pc))
  (assert-eq "rep/period-comparison revenue-change-pct" -56 (:revenue-change-pct pc)))

;; inventory-turnover
;; total-cogs: sum(units-ordered * product-cost) for all products
;; prod1: 7*500=3500, prod2: 2*800=1600, prod3: 10*200=2000
;; prod4: 3*350=1050, prod5: 20*50=1000, prod6: 1*1200=1200, prod7: 0*300=0
;; total-cogs = 3500+1600+2000+1050+1000+1200+0 = 10350
;; avg-inv-value = 247600
;; turnover-ratio = 10350*100/247600 = 4
(let [it (rep/inventory-turnover orders stock-levels products)]
  (assert-eq "rep/inventory-turnover cogs" 10350 (:cost-of-goods-sold it))
  (assert-eq "rep/inventory-turnover avg-inv-value" 247600 (:avg-inventory-value it))
  (assert-eq "rep/inventory-turnover ratio" 4 (:turnover-ratio it)))

;; inventory-summary-report
(let [isr (rep/inventory-summary-report stock-levels products warehouses)]
  (assert-eq "rep/inventory-summary-report count" 2 (count isr))
  (assert-eq "rep/inventory-summary-report wh1 name" "Main DC" (:warehouse-name (first isr)))
  (assert-eq "rep/inventory-summary-report wh1 items" 6 (:item-count (first isr)))
  (assert-eq "rep/inventory-summary-report wh1 units" 888 (:total-units (first isr)))
  (assert-eq "rep/inventory-summary-report wh1 value" 175100 (:value (first isr)))
  (assert-eq "rep/inventory-summary-report wh1 util" 8 (:utilization-pct (first isr)))
  (assert-eq "rep/inventory-summary-report wh2 name" "West Hub" (:warehouse-name (second isr)))
  (assert-eq "rep/inventory-summary-report wh2 units" 400 (:total-units (second isr)))
  (assert-eq "rep/inventory-summary-report wh2 value" 72500 (:value (second isr))))

;; low-stock-report
(let [lsr (rep/low-stock-report stock-levels products)]
  (assert-eq "rep/low-stock-report count" 2 (count lsr))
  ;; Items are sl-1-6 (Gear Set) and sl-2-3 (Stapler)
  (let [by-pid (into {} (mapv (fn [r] [(:product-id r) r]) lsr))]
    (assert-eq "rep/low-stock-report Gear Set qty" 8 (:quantity (get by-pid 6)))
    (assert-eq "rep/low-stock-report Gear Set deficit" 2 (:deficit (get by-pid 6)))
    (assert-eq "rep/low-stock-report Stapler qty" 0 (:quantity (get by-pid 3)))
    (assert-eq "rep/low-stock-report Stapler deficit" 30 (:deficit (get by-pid 3)))))

;; product-sales-report (active products only, 6 items)
(let [psr (rep/product-sales-report orders products)]
  (assert-eq "rep/product-sales-report count" 6 (count psr)))

;; top-products-by-revenue: top 2
;; Revenues: Widget A=8400, Stapler=4500, Gadget B=3000, Bolt Pack=2400, Desk Lamp=2250, Gear Set=2200
(let [top2 (rep/top-products-by-revenue orders products 2)]
  (assert-eq "rep/top-products-by-revenue #1" "Widget A" (:product-name (first top2)))
  (assert-eq "rep/top-products-by-revenue #2" "Stapler" (:product-name (second top2))))

;; top-products-by-units: top 2
;; Units: Bolt Pack=20, Stapler=10, Widget A=7, Desk Lamp=3, Gadget B=2, Gear Set=1
(let [top2 (rep/top-products-by-units orders products 2)]
  (assert-eq "rep/top-products-by-units #1" "Bolt Pack" (:product-name (first top2)))
  (assert-eq "rep/top-products-by-units #2" "Stapler" (:product-name (second top2))))

;; customer-report
(let [cr (rep/customer-report customers orders)]
  (assert-eq "rep/customer-report count" 3 (count cr))
  (let [by-name (into {} (mapv (fn [r] [(:customer-name r) r]) cr))]
    (assert-eq "rep/customer-report Alice orders" 2 (:order-count (get by-name "Alice Smith")))
    (assert-eq "rep/customer-report Alice spend" 12576 (:total-spend (get by-name "Alice Smith")))
    (assert-eq "rep/customer-report Bob orders" 1 (:order-count (get by-name "Bob Jones")))
    (assert-eq "rep/customer-report Carol spend" 2508 (:total-spend (get by-name "Carol White")))))

;; top-customers-by-spend
(let [top2 (rep/top-customers-by-spend customers orders 2)]
  (assert-eq "rep/top-customers-by-spend #1" "Alice Smith" (:customer-name (first top2)))
  (assert-eq "rep/top-customers-by-spend #2" "Bob Jones" (:customer-name (second top2))))

;; reorder-report
(let [rr (rep/reorder-report stock-levels products suppliers)]
  (assert-eq "rep/reorder-report count" 2 (count rr))
  (let [by-name (into {} (mapv (fn [r] [(:product-name r) r]) rr))]
    ;; Gear Set: sup=Parts R Us, reorder=4, cost=4*1200=4800, lead=3
    (assert-eq "rep/reorder-report Gear Set supplier" "Parts R Us" (:supplier-name (get by-name "Gear Set")))
    (assert-eq "rep/reorder-report Gear Set qty" 4 (:reorder-qty (get by-name "Gear Set")))
    (assert-eq "rep/reorder-report Gear Set cost" 4800 (:estimated-cost (get by-name "Gear Set")))
    (assert-eq "rep/reorder-report Gear Set lead" 3 (:lead-time (get by-name "Gear Set")))
    ;; Stapler: sup=Widget Co, reorder=60, cost=60*200=12000, lead=14
    (assert-eq "rep/reorder-report Stapler supplier" "Widget Co" (:supplier-name (get by-name "Stapler")))
    (assert-eq "rep/reorder-report Stapler qty" 60 (:reorder-qty (get by-name "Stapler")))
    (assert-eq "rep/reorder-report Stapler cost" 12000 (:estimated-cost (get by-name "Stapler")))
    (assert-eq "rep/reorder-report Stapler lead" 14 (:lead-time (get by-name "Stapler")))))

;; category-performance-report
(let [cpr (rep/category-performance-report orders products categories)]
  (assert-eq "rep/category-performance count" 3 (count cpr))
  (let [by-name (into {} (mapv (fn [r] [(:category-name r) r]) cpr))]
    ;; Electronics: prod1(8400)+prod2(3000)+prod7(0)=11400 revenue, 7+2+0=9 units
    (assert-eq "rep/category-performance Electronics revenue" 11400 (:total-revenue (get by-name "Electronics")))
    (assert-eq "rep/category-performance Electronics units" 9 (:total-units (get by-name "Electronics")))
    ;; Office: prod3(4500)+prod4(2250)=6750 revenue, 10+3=13 units
    (assert-eq "rep/category-performance Office revenue" 6750 (:total-revenue (get by-name "Office")))
    (assert-eq "rep/category-performance Office units" 13 (:total-units (get by-name "Office")))
    ;; Industrial: prod5(2400)+prod6(2200)=4600 revenue, 20+1=21 units
    (assert-eq "rep/category-performance Industrial revenue" 4600 (:total-revenue (get by-name "Industrial")))
    (assert-eq "rep/category-performance Industrial units" 21 (:total-units (get by-name "Industrial")))))

;; supplier-performance-report
(let [spr (rep/supplier-performance-report orders products suppliers)]
  (assert-eq "rep/supplier-performance count" 3 (count spr))
  (let [by-name (into {} (mapv (fn [r] [(:supplier-name r) r]) spr))]
    ;; Acme Corp (sup1): prod1+prod2+prod7 -> 8400+3000+0=11400, 7+2+0=9 units
    (assert-eq "rep/supplier-performance Acme revenue" 11400 (:total-revenue (get by-name "Acme Corp")))
    (assert-eq "rep/supplier-performance Acme units" 9 (:total-units (get by-name "Acme Corp")))
    (assert-eq "rep/supplier-performance Acme lead" 7 (:lead-time (get by-name "Acme Corp")))
    ;; Widget Co (sup2): prod3+prod4 -> 4500+2250=6750, 10+3=13 units
    (assert-eq "rep/supplier-performance Widget Co revenue" 6750 (:total-revenue (get by-name "Widget Co")))
    ;; Parts R Us (sup3): prod5+prod6 -> 2400+2200=4600, 20+1=21 units
    (assert-eq "rep/supplier-performance Parts R Us revenue" 4600 (:total-revenue (get by-name "Parts R Us")))))

;; product-360-report
(let [r (rep/product-360-report prod1 orders stock-levels)]
  (assert-eq "rep/product-360 name" "Widget A" (:name r))
  (assert-eq "rep/product-360 sku" "WGT-001" (:sku r))
  (assert-eq "rep/product-360 price" 1200 (:price r))
  (assert-eq "rep/product-360 cost" 500 (:cost r))
  (assert-eq "rep/product-360 margin" 700 (:margin r))
  (assert-eq "rep/product-360 margin-pct" 58 (:margin-pct r))
  (assert-eq "rep/product-360 units-sold" 7 (:units-sold r))
  (assert-eq "rep/product-360 revenue" 8400 (:revenue r))
  (assert-eq "rep/product-360 stock-on-hand" 175 (:stock-on-hand r))
  (assert-true "rep/product-360 active" (:active r)))

;; executive-summary
(let [es (rep/executive-summary products suppliers categories stock-levels warehouses customers orders)]
  (assert-eq "rep/executive-summary fulfillment-rate" 100 (:fulfillment-rate es))
  (assert-eq "rep/executive-summary top-products count" 3 (count (:top-products es)))
  (assert-eq "rep/executive-summary top-products #1" "Widget A" (first (:top-products es)))
  (assert-eq "rep/executive-summary top-customers count" 3 (count (:top-customers es)))
  (assert-eq "rep/executive-summary top-customers #1" "Alice Smith" (first (:top-customers es))))

;; customer-lifetime-value
(let [clv (rep/customer-lifetime-value cust1 orders 2025)]
  (assert-eq "rep/clv Alice name" "Alice Smith" (:customer-name clv))
  (assert-eq "rep/clv Alice total-spend" 12576 (:total-spend clv))
  (assert-eq "rep/clv Alice tenure" 5 (:tenure-years clv))
  ;; annual-value = 12576 / 5 = 2515
  (assert-eq "rep/clv Alice annual-value" 2515 (:annual-value clv)))

;; tier-revenue-report
(let [trr (rep/tier-revenue-report customers orders)
      by-tier (into {} (mapv (fn [r] [(:tier r) r]) trr))]
  ;; gold: Alice, orders 1001+1003, revenue=8415+4161=12576
  (assert-eq "rep/tier-revenue gold revenue" 12576 (:revenue (get by-tier "gold")))
  (assert-eq "rep/tier-revenue gold customer-count" 1 (:customer-count (get by-tier "gold")))
  (assert-eq "rep/tier-revenue gold order-count" 2 (:order-count (get by-tier "gold")))
  ;; silver: Bob, order 1002, revenue=6831
  (assert-eq "rep/tier-revenue silver revenue" 6831 (:revenue (get by-tier "silver")))
  ;; bronze: Carol, order 1004, revenue=2508
  (assert-eq "rep/tier-revenue bronze revenue" 2508 (:revenue (get by-tier "bronze"))))


;; ===========================================================================
;; Summary
;; ===========================================================================

(println "")
(println "---")
(let [passes @*pass-count*
      fails @*fail-count*]
  (println (str passes " passed, " fails " failed"))
  (when (> fails 0)
    (System/exit 1)))
