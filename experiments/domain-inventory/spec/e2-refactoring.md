# E2: Cross-Module Refactoring — Add Bulk Discount Feature

Starting from the golden reference implementation, add a bulk discount feature
that touches 3 of the 5 modules. This tests whether beagle's type system
catches errors during cross-module modifications vs. Clojure's runtime-only
checking.

## Starting Point

The golden reference code in `golden/beagle/` (or `golden/clojure/`). Copy it
to the response directory before starting.

## Changes Required

### 1. customers.rkt — New function: `customer-bulk-discount-pct`

Add a function that returns an additional bulk discount percentage based on
the customer's purchase history:

```
customer-bulk-discount-pct [records : Any, cust-id : Long] : Long
```

Logic:
- If total spend >= 200000 (cents): 10% additional
- If total spend >= 100000: 5% additional
- Otherwise: 0%

This is ON TOP of the existing tier discount (from `tier-discount-pct`).

### 2. orders.rkt — Modify `create-order` to apply combined discounts

Modify the order creation to calculate the total discount as:
`tier-discount + bulk-discount` (capped at 30% to prevent over-discounting).

Change `customer-discount-pct` to return the COMBINED discount:
```
combined-discount-pct [customers records cust-id] : Long
  tier-discount = cust/tier-discount-pct(customer-tier)
  bulk-discount = cust/customer-bulk-discount-pct(records, cust-id)
  min(tier-discount + bulk-discount, 30)
```

Also add `purchase-records` as a new parameter to `create-order`:
```
create-order [id cust-id line-specs products categories customers
              purchase-records timestamp] : Order
```

### 3. reports.rkt — New function: `discount-analysis-report`

Add a report that shows the discount breakdown per customer:
```
discount-analysis-report [customers orders purchase-records] : Vec<Map>
```

Each entry:
```
{:customer-name String
 :tier String
 :tier-discount Long
 :bulk-discount Long
 :combined-discount Long
 :total-discount-amount Long}
```

Where `total-discount-amount` is the sum of `order-discount` across all
orders for that customer.

## Verification

The E2 verify script checks:
1. `customer-bulk-discount-pct` returns correct values for each test customer
2. `create-order` with the new parameter produces correct combined discounts
3. Combined discount is capped at 30%
4. `discount-analysis-report` produces correct breakdown
5. All existing golden verify assertions still pass (no regressions)

## What This Tests

- **Cross-module contract change**: `create-order` signature changes, callers
  in reports.rkt must update
- **New cross-module function call**: `cust/customer-bulk-discount-pct` called
  from orders.rkt with correct types
- **Regression risk**: changing `customer-discount-pct` must not break existing
  order calculations in reports
- **Beagle advantage**: if the agent passes wrong types to the new function or
  forgets to update callers, beagle catches at compile time; Clojure fails
  silently or at runtime
