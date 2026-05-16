# E3: Bug Detection — 12 Injected Bugs

Starting from the golden reference, 12 bugs are injected across the modules.
The LLM agent is given the buggy code and asked to find and fix all bugs.
Measures: how many bugs found, how quickly, beagle compile errors vs. runtime
failures.

## Bug Classification

| #  | Module    | Bug Type              | Beagle Catches? | Description |
|----|-----------|----------------------|-----------------|-------------|
| 1  | inventory | arity error          | YES             | `cat/find-product-by-id` called with 1 arg instead of 2 |
| 2  | orders    | arity error          | YES             | `inv/can-fulfill?` called with 2 args instead of 3 |
| 3  | reports   | arity error          | YES             | `ord/customer-total-spend` called with 1 arg instead of 2 |
| 4  | orders    | arity error          | YES             | `cust/tier-discount-pct` called with 2 args instead of 1 |
| 5  | inventory | wrong field accessor | YES             | `cat/product-name` used where `cat/product-unit-cost` needed in stock valuation |
| 6  | orders    | wrong field accessor | YES             | `cust/customer-name` used where `cust/customer-tier` needed for discount |
| 7  | reports   | wrong field accessor | YES             | `cat/product-unit-cost` used where `cat/product-unit-price` needed in revenue calc |
| 8  | orders    | wrong type passed    | YES             | Customer record passed where product-id (Long) expected |
| 9  | reports   | wrong type passed    | YES             | String literal passed where Long expected in comparison |
| 10 | catalog   | logic error          | NO              | `product-margin` subtracts cost from cost instead of price from cost |
| 11 | customers | logic error          | NO              | `tier-discount-pct` returns 5 for gold instead of 15 |
| 12 | orders    | logic error          | NO              | `calculate-total` adds discount instead of subtracting |

## Expected Results

- **Bugs 1-9**: Beagle catches at compile time with clear error messages.
  Clojure compiles fine; bugs surface only at runtime (or never, if untested).
- **Bugs 10-12**: Neither language catches these — they're semantic/logic
  errors that produce wrong values. Both tracks need the verify script.

## Bug Details

### Bug 1: Arity — inventory.rkt `stock-value-at-warehouse`
```diff
- (let [prod (cat/find-product-by-id products (stocklevel-product-id sl))
+ (let [prod (cat/find-product-by-id (stocklevel-product-id sl))
```
Missing `products` argument.

### Bug 2: Arity — orders.rkt `can-fulfill-line?`
```diff
- (inv/can-fulfill? levels (orderline-product-id ol) (orderline-quantity ol))
+ (inv/can-fulfill? levels (orderline-product-id ol))
```
Missing `quantity` argument.

### Bug 3: Arity — reports.rkt `customer-report`
```diff
- (let [cid (cust/customer-id c)
-       order-count (ord/customer-order-count orders cid)
-       total-spend (ord/customer-total-spend orders cid)
+ (let [cid (cust/customer-id c)
+       order-count (ord/customer-order-count orders cid)
+       total-spend (ord/customer-total-spend orders)
```
Missing `cust-id` argument.

### Bug 4: Arity — orders.rkt `customer-discount-pct`
```diff
- (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-tier c)))
+ (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-tier c) cust-id))
```
Extra argument to `tier-discount-pct`.

### Bug 5: Wrong accessor — inventory.rkt `stock-value-at-warehouse`
```diff
-               cost (if (nil? prod) 0 (cat/product-unit-cost prod))]
+               cost (if (nil? prod) 0 (cat/product-name prod))]
```
Returns String instead of Long — type mismatch in multiplication.

### Bug 6: Wrong accessor — orders.rkt `customer-discount-pct`
```diff
- (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-tier c)))
+ (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-name c)))
```
Passes name (String) instead of tier (String) — same type but wrong value.
Actually beagle won't catch this since both are String. Change bug:
```diff
- (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-tier c)))
+ (if (nil? c) 0 (cust/tier-discount-pct (cust/customer-id c)))
```
Passes id (Long) instead of tier (String).

### Bug 7: Wrong accessor — reports.rkt `product-profitability`
```diff
-                cost-of-goods (* units (cat/product-unit-cost p))
+                cost-of-goods (* units (cat/product-unit-price p))
```
Uses price instead of cost for COGS — same type (Long), produces wrong value.
This is actually a logic bug (type matches). Replace with:
```diff
-                revenue (ord/product-revenue orders pid)
+                revenue (ord/product-revenue orders (cat/product-name p))
```
Passes String (name) where Long (pid) expected.

### Bug 8: Wrong type — orders.rkt `create-order`
```diff
-       disc-pct (customer-discount-pct customers cust-id)
+       disc-pct (customer-discount-pct customers cust-id)
```
Actually inject in make-order-line:
```diff
- (let [prod (cat/find-product-by-id products prod-id)
+ (let [prod (cat/find-product-by-id products customers)
```
Passes customers collection where Long (prod-id) expected.

### Bug 9: Wrong type — reports.rkt `fulfillment-report`
```diff
-              total (ord/order-total o)
+              total (ord/order-total "pending")
```
Passes String where Order expected.

### Bug 10: Logic — catalog.rkt `product-margin`
```diff
- (- (product-unit-price p) (product-unit-cost p))
+ (- (product-unit-cost p) (product-unit-cost p))
```
Always returns 0.

### Bug 11: Logic — customers.rkt `tier-discount-pct`
```diff
-     [(= tier "gold") 15]
+     [(= tier "gold") 5]
```
Gold customers get bronze-level discount.

### Bug 12: Logic — orders.rkt `calculate-total`
```diff
- (- (+ subtotal tax) discount)
+ (+ (+ subtotal tax) discount)
```
Adds discount instead of subtracting.

## Procedure

1. Create buggy versions by applying all 12 diffs to the golden code
2. Give buggy code to LLM agent with instruction: "This code has bugs. Find
   and fix them. The verify script should pass."
3. Measure: bugs found, bugs fixed correctly, time, tool uses, compile errors
   encountered and used as hints

## Metrics

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Bugs found (of 12) | | |
| Type errors at compile (of 9) | | |
| Logic bugs found (of 3) | | |
| Fix iterations | | |
| Time (seconds) | | |
| Tool calls | | |
