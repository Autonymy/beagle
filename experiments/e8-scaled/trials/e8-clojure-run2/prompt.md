# Task: Fix bugs in a 13-module Clojure inventory system

You have a large inventory & order management system (13 modules, ~4700 LOC) written in Clojure. The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (291 assertions).

## Workflow

1. **Run the verify script** to see which assertions fail
2. **Read the failing modules** and fix the bugs
3. **Re-run verify** until all 291 assertions pass

To run verify:
```bash
clojure -Sdeps '{:paths ["."]}' -M -e '(load-file "../../verify/e8.verify.clj")'
```

## Available tools

All tools are at `/home/tom/code/beagle/bin/`:

- `/home/tom/code/beagle/bin/clj-sig FUNCTION *.clj` — show a function's signature (parameter names)
- `/home/tom/code/beagle/bin/clj-fields RECORD *.clj` — show record fields
- `/home/tom/code/beagle/bin/clj-callers FUNCTION *.clj` — find all call sites
- `/home/tom/code/beagle/bin/clj-provides *.clj` — list all exports from a module

To run verify:
```bash
clojure -Sdeps '{:paths ["."]}' -M -e '(load-file "../../verify/e8.verify.clj")'
```

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

## Important

- Read the verification script carefully — it defines correct behavior for all 291 assertions
- Pay attention to which arguments go where — many functions take collection + id pairs
- Watch for: wrong accessor usage, swapped arguments, missing arguments, wrong arithmetic, wrong comparisons
- Cross-module calls are common — `ord/order-total`, `cat/product-unit-cost`, `ship/shipment-id` etc.
- The verify script is the final oracle — all 291 assertions must pass
- Use `clj-sig` and `clj-callers` to understand function contracts without reading entire files
