# Task: Fix bugs in a 13-module Clojure inventory system

You have a large inventory & order management system (13 modules, ~4700 LOC) written in Clojure. The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow — IMPORTANT: follow this order

Your primary diagnostic tool is **clj-kondo**. Use it first, every time.

1. **Run clj-kondo** to find arity and structural errors:
   ```
   clj-kondo --lint *.clj
   ```
   Fix ALL clj-kondo errors before proceeding. These are real bugs — wrong number of arguments, undefined references. Each error is a mechanical fix: read the error, fix the code, re-run clj-kondo until zero errors.

   Note: clj-kondo warnings (unused bindings, redundant do, etc.) are NOT bugs — ignore them. Only fix errors.

2. **Run the verify script** to find remaining logic bugs:
   ```
   clojure -Sdeps '{:paths ["."]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
   ```

3. **Fix logic bugs** using verify output, then re-run verify until all 484 assertions pass.

4. **After every batch of fixes**, re-run clj-kondo to make sure you haven't introduced new arity errors, then re-run verify.

Do NOT skip clj-kondo and go straight to verify. The clj-kondo errors are structured, precise diagnostics — they tell you exactly which function is called with the wrong arity.

## Available tools

- `clj-kondo --lint *.clj` — static linting (catches arity errors, undefined vars)
- `clojure -Sdeps '{:paths ["."]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'` — behavioral verification (484 assertions)
- `/home/tom/code/beagle/bin/clj-sig FUNCTION *.clj` — show a function's signature (parameter names)
- `/home/tom/code/beagle/bin/clj-fields RECORD *.clj` — show record fields
- `/home/tom/code/beagle/bin/clj-callers FUNCTION *.clj` — find all call sites
- `/home/tom/code/beagle/bin/clj-provides *.clj` — list all exports from a module

## Module dependency DAG (13 modules)

```
Layer 0 (leaves):     catalog, customers
Layer 1:              inventory (→ catalog)
                      orders (→ catalog, inventory, customers)
Layer 2:              reports (→ all layer 0–1)
Layer 3:              shipping (→ orders, inventory, customers, catalog)
                      billing (→ orders, customers, catalog)
                      procurement (→ catalog, inventory)
                      promotions (→ catalog, customers, orders)
                      employees (→ orders, catalog)
Layer 4:              analytics (→ orders, inventory, billing, shipping, catalog)
                      notifications (→ orders, customers, shipping, billing)
Layer 5:              audit (→ all modules)
```

## Domain semantics

- All monetary amounts are in **cents** (Long). $12.00 = 1200
- IDs are Long values
- Timestamps are epoch **seconds** (not millis in this system)
- Days = seconds / 86400
- Percentages: integer 0-100 (e.g., margin-pct 58 means 58%)
- Tax rates are decimal (0.15 = 15%)
- Discount types: "percentage" (value 0-100) or "fixed" (value in cents)
- Record accessors: `record-field` pattern (e.g., `carrier-base-rate`, `invoice-total`)
- Cross-module calls: `alias/function` (e.g., `ord/order-total`, `cat/product-unit-cost`)
- Tier hierarchy: gold > silver > bronze > "" (any)
- Fee basis: bps where noted, pct where noted — read the function spec

## How to interpret clj-kondo errors

```
billing.clj:61:101: error: customers/customer-id is called with 0 args but expects 1
```
→ `customer-id` expects 1 argument but the call passes none. Find line 61 in billing.clj and add the missing argument.

```
analytics.clj:82:114: error: orders/orders-in-period is called with 2 args but expects 3
```
→ Missing an argument. Check the function signature with `clj-sig orders-in-period *.clj` and add the missing arg.

## How to interpret verify failures

```
FAIL: cat/product-margin Widget A
  expected: 700
  actual:   -700
```
→ `product-margin` returns -700 when it should return 700. Read the function, understand the computation, fix the logic.

## Important

- Run clj-kondo FIRST. It catches 5 arity errors — these are fast, mechanical fixes.
- After clj-kondo is clean, the remaining bugs are logic errors and type mismatches (wrong accessor, swapped operands, wrong operator) — use verify for those.
- The verify script is the final oracle — all 484 assertions must pass.
- Use `clj-sig` and `clj-callers` to understand function contracts without reading entire files.
