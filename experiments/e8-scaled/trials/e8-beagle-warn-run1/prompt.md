# Task: Fix bugs in a 13-module beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow

1. **Compile with `--warn` and run verify** to see what fails at runtime:
   ```bash
   /home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ --warn && clojure -Sdeps '{:paths [".build"]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
   ```
2. **Use type warnings as a repair guide** — the compiler prints diagnostics for type errors even though it emits code. These diagnostics tell you exactly what's wrong and suggest fixes.
3. **Fix bugs iteratively** — prioritize bugs that cause runtime failures or assertion failures. Type warnings with "did you mean X?" suggestions are usually the correct fix.
4. **Re-compile and re-verify** until all 484 assertions pass.

## Key insight: --warn mode

`--warn` makes type errors non-blocking. The compiler:
- Still prints all type diagnostics (wrong accessor, arity, scalar mismatch)
- Still suggests fixes ("did you mean X?")
- But emits Clojure anyway, so you can run and verify immediately

This means you can see BOTH type warnings AND runtime/assertion failures in one pass. Use type warnings to fix bugs efficiently — they pinpoint the exact location and usually suggest the fix.

## Additional repair tools

- `/home/tom/code/beagle/bin/beagle-fix --apply .` — auto-fix high-confidence single-suggestion type errors (run this first for quick wins)
- `/home/tom/code/beagle/bin/beagle-check-all .` — type-check all 13 files (detailed diagnostics without compiling)
- `/home/tom/code/beagle/bin/beagle-sig FN-NAME .` — print function's type signature
- `/home/tom/code/beagle/bin/beagle-fields RECORD .` — print record fields + accessor types
- `/home/tom/code/beagle/bin/beagle-callers FN-NAME .` — find all call sites
- `/home/tom/code/beagle/bin/beagle-provides FILE` — list all exports with types

## Suggested approach

1. Run `beagle-fix --apply .` first to auto-fix obvious type errors
2. Then compile with `--warn` and run verify
3. For each failing assertion or runtime error, check if there's a matching type warning — if so, apply the suggested fix
4. For logic bugs (no type warning), reason from the assertion's expected vs actual values
5. Re-verify after each batch of fixes

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
- Nominal scalar types: `ProductId`, `CategoryId`, `SupplierId`, `Price` (catalog); `CustomerId` (customers); `WarehouseId` (inventory); `OrderId`, `Timestamp`, `Amount` (orders); `ShipmentId`, `CarrierId`, `Weight` (shipping); `InvoiceId`, `PaymentId` (billing); `POId` (procurement); `CampaignId`, `CouponId` (promotions); `EmployeeId` (employees); `NotificationId`, `TemplateId` (notifications)
- Scalar constructors: `(->ProductId x)`, accessors: `(productid-value x)` — wrap/unwrap for arithmetic
- Timestamps are epoch **seconds** (not millis)
- Days = seconds / 86400
- Percentages: integer 0-100
- Tax rates are decimal (0.15 = 15%)
- Record accessors: `record-field` pattern (e.g., `carrier-base-rate`, `invoice-total`)
- Cross-module calls: `alias/function` (e.g., `ord/order-total`, `cat/product-unit-cost`)

## How to interpret type warnings

Each warning tells you exactly what's wrong:
- **E001 (arity)**: wrong number of arguments — the `sig:` line shows expected params
- **E002 (type mismatch)**: wrong type at a specific arg position — `help:` suggests the right accessor/function
- **`did you mean X?`** hints are almost always the correct fix
- Single suggestions → very high confidence, apply directly
- Multiple suggestions → pick the one that matches the function's intent

## beagle language reference

```racket
#lang beagle
(ns my.namespace)
(define-mode strict)
(require some.module :as mod)

;; Scalar types (nominal wrappers over Long)
(defscalar Amount Long)
;; Constructor: (->Amount 5000) typed [Long -> Amount]
;; Accessor: (amount-value x) typed [Amount -> Long]
;; Arithmetic: (+ (amount-value a) (amount-value b))
;; Rewrap: (->Amount result)
```

## Important

- Compile with `--warn` so you can run verify immediately alongside type diagnostics
- `beagle-fix --apply .` is a free speedup — run it first
- Type warnings + oracle failures give you two complementary signals
- The verify script has 484 assertions — all must pass
