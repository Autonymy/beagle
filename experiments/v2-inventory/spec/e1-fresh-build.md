# E1: Fresh Build — Inventory & Order Management System

Build a complete Inventory & Order Management System from scratch across 5
modules. Each module lives in its own file. Modules form a DAG:

```
catalog (leaf)        customers (leaf)
   |                      |
   +--- inventory ---------+
   |         |             |
   +--- orders ------------+
             |
         reports (requires all)
```

---

## Part 1: Domain Specification (language-neutral)

### Module: catalog

Records:

| Record    | Fields                                                                                       |
|-----------|----------------------------------------------------------------------------------------------|
| Supplier  | id:Long, name:String, lead-time-days:Long                                                    |
| Category  | id:Long, name:String, tax-rate:Double                                                        |
| Product   | id:Long, name:String, sku:String, unit-cost:Long, unit-price:Long, supplier-id:Long, category-id:Long, active:Boolean |
| PriceTier | min-qty:Long, discount-pct:Long                                                              |

All monetary values are in **cents** (Long). `discount-pct` is a whole-number
percentage (e.g. 10 means 10%).

Functions (grouped by category):

**Lookup & filtering:**
- `find-product-by-id [products id] -> Product|Nil` — linear scan
- `find-product-by-sku [products sku] -> Product|Nil` — linear scan
- `find-products-by-supplier [products supplier-id] -> Vec<Product>` — filter
- `find-products-by-category [products category-id] -> Vec<Product>` — filter
- `active-products [products] -> Vec<Product>` — filter where active=true
- `find-supplier-by-id [suppliers id] -> Supplier|Nil` — linear scan
- `find-category-by-id [categories id] -> Category|Nil` — linear scan

**Pricing:**
- `gross-margin [product] -> Long` — unit-price minus unit-cost
- `margin-pct [product] -> Double` — margin / unit-price as decimal (0.0-1.0)
- `apply-discount [price discount-pct] -> Long` — reduce price by percentage, truncate to Long
- `tiered-price [unit-price tiers quantity] -> Long` — find highest tier where min-qty <= quantity, apply its discount to unit-price; if no tier qualifies return unit-price unchanged
- `line-total [unit-price quantity] -> Long` — unit-price * quantity

**Aggregation:**
- `total-catalog-value [products] -> Long` — sum of (unit-price * 1) for all active products
- `avg-price-by-category [products category-id] -> Long` — average unit-price of active products in category, truncate
- `most-expensive-product [products] -> Product|Nil` — highest unit-price among active
- `cheapest-product [products] -> Product|Nil` — lowest unit-price among active

**Bulk operations:**
- `mark-inactive [products id] -> Vec<Product>` — return new vec with that product's active set to false
- `adjust-prices [products category-id pct-change] -> Vec<Product>` — multiply unit-price of matching active products by (1 + pct-change/100), truncate; leave others unchanged

**Comparisons:**
- `cheaper-than [products max-price] -> Vec<Product>` — active products with unit-price < max-price
- `price-between [products min-price max-price] -> Vec<Product>` — active products with unit-price in [min, max] inclusive
- `products-sorted-by-price [products] -> Vec<Product>` — active products sorted ascending by unit-price
- `products-sorted-by-margin [products] -> Vec<Product>` — active products sorted descending by margin-pct

---

### Module: customers

Records:

| Record          | Fields                                                                     |
|-----------------|---------------------------------------------------------------------------|
| Address         | street:String, city:String, state:String, zip:String                       |
| Customer        | id:Long, name:String, email:String, tier:String, address:Address, created-year:Long |
| PurchaseRecord  | customer-id:Long, order-id:Long, amount:Long, year:Long                    |
| LoyaltyBalance  | customer-id:Long, points:Long, lifetime-points:Long                        |

Tier values: "gold", "silver", "bronze".

Functions:

**Lookup:**
- `find-customer-by-id [customers id] -> Customer|Nil`
- `find-customer-by-email [customers email] -> Customer|Nil`
- `customers-by-tier [customers tier] -> Vec<Customer>`
- `customers-in-state [customers state] -> Vec<Customer>` — match address.state

**Tier logic:**
- `tier-discount-pct [tier] -> Long` — gold=15, silver=10, bronze=5
- `upgrade-tier [customer] -> Customer` — bronze->silver, silver->gold, gold stays gold
- `downgrade-tier [customer] -> Customer` — gold->silver, silver->bronze, bronze stays bronze

**Purchase history:**
- `customer-purchases [records customer-id] -> Vec<PurchaseRecord>` — filter
- `total-spent [records customer-id] -> Long` — sum of amount for that customer
- `total-spent-in-year [records customer-id year] -> Long` — sum for customer in year
- `purchase-count [records customer-id] -> Long` — number of records

**Loyalty:**
- `earn-points [balance amount] -> LoyaltyBalance` — add amount/100 (truncate) to points and lifetime-points
- `redeem-points [balance points-to-redeem] -> LoyaltyBalance|Nil` — subtract from points if sufficient, return nil if insufficient
- `points-to-dollars [points] -> Long` — 100 points = 100 cents (1 dollar); points * 1

**Segmentation & retention:**
- `high-value-customers [records customers threshold] -> Vec<Customer>` — customers whose total-spent across all records >= threshold
- `at-risk-customers [records customers current-year inactive-years] -> Vec<Customer>` — customers with no purchase in the last inactive-years years
- `customer-tenure [customer current-year] -> Long` — current-year minus created-year

---

### Module: inventory

Requires: catalog

Records:

| Record        | Fields                                                                                           |
|---------------|--------------------------------------------------------------------------------------------------|
| Warehouse     | id:Long, name:String, location:String, capacity:Long                                             |
| StockLevel    | warehouse-id:Long, product-id:Long, quantity:Long, min-quantity:Long                             |
| StockMovement | id:Long, warehouse-id:Long, product-id:Long, quantity-change:Long, movement-type:String, timestamp:Long |
| Transfer      | id:Long, from-warehouse:Long, to-warehouse:Long, product-id:Long, quantity:Long, timestamp:Long  |

movement-type values: "inbound", "outbound", "adjustment".

Functions:

**Stock queries:**
- `stock-for-product [levels warehouse-id product-id] -> StockLevel|Nil`
- `total-stock-for-product [levels product-id] -> Long` — sum quantity across all warehouses
- `out-of-stock [levels] -> Vec<StockLevel>` — where quantity = 0
- `below-minimum [levels] -> Vec<StockLevel>` — where quantity < min-quantity (and quantity > 0)
- `warehouse-utilization [levels warehouse] -> Double` — sum of quantities in that warehouse / warehouse capacity

**Valuation:**
- `stock-value [levels products warehouse-id] -> Long` — sum of (quantity * unit-cost) for each stock level in warehouse, looking up unit-cost from products by product-id
- `total-inventory-value [levels products] -> Long` — sum across all warehouses

**Reorder:**
- `needs-reorder [levels] -> Vec<StockLevel>` — where quantity <= min-quantity
- `reorder-quantity [level] -> Long` — (min-quantity * 2) - quantity; i.e. restock to 2x min
- `generate-reorder-list [levels products] -> Vec` — for each needs-reorder level, produce a map/record with product-id, product-name (from products), current quantity, reorder-quantity

**Movements:**
- `record-movement [movements id warehouse-id product-id qty-change type timestamp] -> Vec<StockMovement>` — append new movement to list
- `apply-movement [levels warehouse-id product-id qty-change] -> Vec<StockLevel>` — update the matching stock level's quantity (add qty-change; can be negative)
- `movements-for-product [movements product-id] -> Vec<StockMovement>` — filter
- `net-movement [movements product-id] -> Long` — sum of quantity-change for a product across all movements

**Transfers:**
- `create-transfer [transfers id from to product-id qty timestamp] -> Vec<Transfer>` — append
- `apply-transfer [levels from-warehouse to-warehouse product-id qty] -> Vec<StockLevel>` — decrement from-warehouse, increment to-warehouse
- `transfers-for-warehouse [transfers warehouse-id] -> Vec<Transfer>` — where from or to matches

**ABC analysis:**
- `classify-abc [levels products] -> Vec` — for each product, compute total-stock-value (quantity * unit-cost across all warehouses). Sort descending. Top 20% by cumulative value = "A", next 30% = "B", rest = "C". Return vec of {product-id, classification} entries.

**Batch operations:**
- `bulk-receive [levels warehouse-id product-quantities] -> Vec<StockLevel>` — product-quantities is a vec of {product-id, quantity} pairs; add each quantity to matching stock level
- `snapshot-stock [levels] -> Vec<StockLevel>` — identity (returns levels unchanged); used as checkpoint marker

---

### Module: orders

Requires: catalog, inventory, customers

Records:

| Record        | Fields                                                                                                    |
|---------------|----------------------------------------------------------------------------------------------------------|
| OrderLine     | product-id:Long, quantity:Long, unit-price:Long, line-total:Long                                          |
| Order         | id:Long, customer-id:Long, status:String, lines:Vec, subtotal:Long, tax:Long, discount:Long, total:Long, created-at:Long |
| ReturnRequest | id:Long, order-id:Long, product-id:Long, quantity:Long, reason:String, status:String                      |

Order status values: "pending", "confirmed", "shipped", "delivered", "cancelled".
ReturnRequest status values: "requested", "approved", "rejected", "completed".

Functions:

**Order creation:**
- `make-order-line [product quantity] -> OrderLine` — unit-price from product, line-total = unit-price * quantity
- `compute-subtotal [lines] -> Long` — sum of line-totals
- `compute-tax [subtotal tax-rate] -> Long` — subtotal * tax-rate, truncate to Long
- `compute-discount [subtotal discount-pct] -> Long` — subtotal * discount-pct / 100, truncate
- `create-order [id customer-id lines tax-rate discount-pct created-at] -> Order` — builds complete order with computed subtotal/tax/discount/total; status="pending"

**Status transitions:**
- `confirm-order [order] -> Order` — pending -> confirmed
- `ship-order [order] -> Order` — confirmed -> shipped
- `deliver-order [order] -> Order` — shipped -> delivered
- `cancel-order [order] -> Order` — pending or confirmed -> cancelled (return unchanged if already shipped/delivered)

**Queries & filtering:**
- `find-order-by-id [orders id] -> Order|Nil`
- `orders-by-customer [orders customer-id] -> Vec<Order>`
- `orders-by-status [orders status] -> Vec<Order>`
- `orders-in-period [orders start-ts end-ts] -> Vec<Order>` — where created-at in [start, end] inclusive
- `order-contains-product [order product-id] -> Boolean` — true if any line has that product-id

**Aggregation:**
- `total-revenue [orders] -> Long` — sum of total for non-cancelled orders
- `avg-order-value [orders] -> Long` — total-revenue / count of non-cancelled orders, truncate
- `largest-order [orders] -> Order|Nil` — highest total among non-cancelled
- `order-count-by-status [orders] -> Vec` — vec of {status, count} entries

**Returns:**
- `create-return [id order-id product-id quantity reason] -> ReturnRequest` — status="requested"
- `approve-return [request] -> ReturnRequest` — requested -> approved
- `reject-return [request] -> ReturnRequest` — requested -> rejected
- `complete-return [request] -> ReturnRequest` — approved -> completed
- `return-value [request products] -> Long` — quantity * product unit-price (looked up from products)

**Partial fulfillment:**
- `fulfillable-lines [order levels warehouse-id] -> Vec<OrderLine>` — lines where stock quantity >= line quantity in the given warehouse
- `unfulfillable-lines [order levels warehouse-id] -> Vec<OrderLine>` — complement of above
- `split-order [order levels warehouse-id] -> Vec<Order>` — split into two orders: one with fulfillable lines (status confirmed), one with unfulfillable (status pending); preserve original id on fulfillable, use id+1000 on backorder

**Order modification:**
- `add-line [order product quantity] -> Order` — append line, recompute subtotal/total (keep same tax-rate implied by existing tax/subtotal ratio and discount-pct implied by discount/subtotal ratio)
- `remove-line [order product-id] -> Order` — remove line with that product-id, recompute

---

### Module: reports

Requires: catalog, inventory, customers, orders

No new records. All functions are pure aggregation/computation.

Functions:

**Inventory reports:**
- `inventory-summary [levels products] -> Vec` — one entry per product: {product-id, product-name, total-quantity, total-value}
- `low-stock-report [levels products] -> Vec` — for below-minimum levels: {product-id, product-name, warehouse-id, quantity, min-quantity, shortfall}
- `stock-by-warehouse [levels products warehouses] -> Vec` — one entry per warehouse: {warehouse-id, warehouse-name, item-count, total-value}

**Sales reports:**
- `sales-by-product [orders products] -> Vec` — for each product appearing in non-cancelled orders: {product-id, product-name, units-sold, revenue}; sorted descending by revenue
- `sales-by-category [orders products categories] -> Vec` — aggregate revenue by category: {category-id, category-name, revenue}
- `top-sellers [orders products n] -> Vec` — top n products by units-sold

**Customer reports:**
- `customer-summary [orders customers] -> Vec` — per customer: {customer-id, name, order-count, total-spent}; only non-cancelled orders
- `revenue-by-tier [orders customers] -> Vec` — aggregate total by customer tier: {tier, revenue, customer-count}
- `clv-estimate [records customer-id current-year] -> Long` — (total-spent / tenure-years) * 5; estimate 5-year value. If tenure is 0, use 1.

**Category & supplier:**
- `revenue-by-category [orders products categories] -> Vec` — {category-id, name, revenue}; non-cancelled orders
- `supplier-performance [orders products suppliers] -> Vec` — per supplier: {supplier-id, name, units-sold, revenue}

**Fulfillment:**
- `fulfillment-rate [orders] -> Double` — count of delivered / count of (delivered + shipped + confirmed + pending); exclude cancelled
- `avg-lines-per-order [orders] -> Double` — total lines across non-cancelled orders / count of non-cancelled orders
- `cancellation-rate [orders] -> Double` — cancelled count / total count

**Profitability:**
- `gross-profit [orders products] -> Long` — for each line in non-cancelled orders: (line unit-price - product unit-cost) * quantity; sum all
- `profit-by-product [orders products] -> Vec` — per product: {product-id, name, profit, margin-pct}
- `profit-by-category [orders products categories] -> Vec` — per category: {category-id, name, profit}

**Period comparison:**
- `period-revenue [orders start-ts end-ts] -> Long` — total-revenue of orders in period
- `period-comparison [orders period1-start period1-end period2-start period2-end] -> Vec` — {period1-revenue, period2-revenue, change-pct}; change-pct = (p2-p1)/p1 * 100 as Long

**Turnover:**
- `inventory-turnover [orders levels products period-start period-end] -> Double` — COGS in period / avg inventory value. COGS = sum of (unit-cost * quantity) for lines in non-cancelled orders in period. Avg inventory value = total-inventory-value from levels (snapshot).

---

## Part 2: Test Data

All verification runs use exactly this data set.

### Suppliers
```
(1, "Acme Corp", 7)
(2, "Widget Co", 14)
(3, "Parts R Us", 3)
```

### Categories
```
(1, "Electronics", 0.15)
(2, "Office", 0.08)
(3, "Industrial", 0.12)
```

### Products
```
(1, "Widget A",    "WGT-001", 500,  1200, 1, 1, true)
(2, "Gadget B",    "GDG-002", 800,  1500, 1, 1, true)
(3, "Stapler",     "STP-003", 200,  450,  2, 2, true)
(4, "Desk Lamp",   "DLM-004", 350,  750,  2, 2, true)
(5, "Bolt Pack",   "BLT-005", 50,   120,  3, 3, true)
(6, "Gear Set",    "GRS-006", 1200, 2200, 3, 3, true)
(7, "Old Widget",  "OWG-007", 300,  600,  1, 1, false)
```

### Warehouses
```
(1, "Main DC", "Chicago", 10000)
(2, "West Hub", "Portland", 5000)
```

### StockLevels
```
(1, 1, 100, 20)   -- Main DC, Widget A, 100 units, min 20
(1, 2, 50,  15)   -- Main DC, Gadget B
(1, 3, 200, 50)   -- Main DC, Stapler
(1, 4, 30,  10)   -- Main DC, Desk Lamp
(1, 5, 500, 100)  -- Main DC, Bolt Pack
(1, 6, 8,   10)   -- Main DC, Gear Set — BELOW MINIMUM
(2, 1, 75,  20)   -- West Hub, Widget A
(2, 2, 25,  10)   -- West Hub, Gadget B
(2, 3, 0,   30)   -- West Hub, Stapler — OUT OF STOCK
(2, 5, 300, 50)   -- West Hub, Bolt Pack
```

### Customers
```
(1, "Alice Smith", "alice@example.com", "gold",   Address("123 Main St", "Chicago",  "IL", "60601"), 2020)
(2, "Bob Jones",   "bob@example.com",   "silver", Address("456 Oak Ave", "Portland", "OR", "97201"), 2021)
(3, "Carol White", "carol@example.com", "bronze", Address("789 Pine Rd", "Austin",   "TX", "78701"), 2023)
```

### PurchaseRecords
```
(1, 101, 15000, 2024)
(1, 102, 22000, 2024)
(1, 103, 18000, 2025)
(2, 201, 8000,  2024)
(2, 202, 12000, 2025)
(3, 301, 5000,  2025)
```

### LoyaltyBalances
```
(1, 550, 1200)
(2, 200, 400)
(3, 50,  50)
```

### Orders (for verification)
```
Order 1: customer=1, status="delivered", created-at=1000
  Lines: [(product 1, qty 2, price 1200, total 2400),
          (product 3, qty 5, price 450,  total 2250)]
  subtotal=4650, tax=697 (15% rate), discount=697 (15% gold), total=4650

Order 2: customer=2, status="shipped", created-at=2000
  Lines: [(product 2, qty 1, price 1500, total 1500),
          (product 5, qty 10, price 120, total 1200)]
  subtotal=2700, tax=405 (15%), discount=270 (10% silver), total=2835

Order 3: customer=3, status="pending", created-at=3000
  Lines: [(product 4, qty 2, price 750, total 1500)]
  subtotal=1500, tax=120 (8%), discount=75 (5% bronze), total=1545

Order 4: customer=1, status="cancelled", created-at=1500
  Lines: [(product 6, qty 1, price 2200, total 2200)]
  subtotal=2200, tax=264 (12%), discount=330 (15% gold), total=2134
```

---

## Part 3: Track-Specific Notes

### Beagle track

File header for each module:
```racket
#lang beagle

(ns inventory.catalog)
(define-mode strict)
```

Cross-module imports use `:as` alias:
```racket
(require inventory.catalog :as cat)
```

All records must have typed fields:
```racket
(defrecord Supplier [(id : Long) (name : String) (lead-time-days : Long)])
```

All functions must have typed parameters and return type:
```racket
(defn find-product-by-id [(products : (Vec Product)) (id : Long)] : (U Product Nil)
  ...)
```

Use `(U X Nil)` for nullable returns. Use `(Vec X)` for collections.

One file per module:
- `catalog.rkt`
- `customers.rkt`
- `inventory.rkt`
- `orders.rkt`
- `reports.rkt`

### Clojure track

Standard Clojure namespace and defrecord:
```clojure
(ns inventory.catalog)

(defrecord Supplier [id name lead-time-days])
```

Standard defn with no type annotations:
```clojure
(defn find-product-by-id [products id]
  ...)
```

Cross-module requires:
```clojure
(ns inventory.orders
  (:require [inventory.catalog :as cat]
            [inventory.inventory :as inv]
            [inventory.customers :as cust]))
```

One file per module:
- `catalog.clj`
- `customers.clj`
- `inventory.clj`
- `orders.clj`
- `reports.clj`
