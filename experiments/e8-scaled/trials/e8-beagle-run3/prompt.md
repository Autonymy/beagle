# Task: Fix bugs in a 13-module beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow

1. **Run `beagle-fix --apply .`** — this auto-fixes high-confidence type errors (single accessor swaps)
2. **Run `beagle-check-all .`** — verify remaining errors and fix them manually
3. **After type errors are cleared**, compile and run verify:
   ```bash
   /home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ && clojure -Sdeps '{:paths [".build"]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
   ```
4. **Iterate** fixing and re-verifying until all 484 assertions pass

## Step 1: Auto-fix mechanical type errors

```bash
/home/tom/code/beagle/bin/beagle-fix --apply .
```

This tool automatically applies high-confidence fixes:
- **AUTO-FIXED**: single unambiguous accessor replacement (applied in-place)
- **SUGGESTED**: likely fix but needs your review (multiple candidates or arity)
- **DIAGNOSTIC**: something is wrong, intent unclear

After running beagle-fix, many mechanical type errors will already be resolved.

## Available tools

All tools are at `/home/tom/code/beagle/bin/`:

- `/home/tom/code/beagle/bin/beagle-fix --apply .` — auto-fix high-confidence type errors
- `/home/tom/code/beagle/bin/beagle-fix --dry-run .` — preview what would be fixed
- `/home/tom/code/beagle/bin/beagle-check-all .` — type-check all 13 files
- `/home/tom/code/beagle/bin/beagle-sig FN-NAME .` — print function's type signature
- `/home/tom/code/beagle/bin/beagle-fields RECORD .` — print record fields + accessor types
- `/home/tom/code/beagle/bin/beagle-callers FN-NAME .` — find all call sites
- `/home/tom/code/beagle/bin/beagle-provides FILE` — list all exports with types
- `/home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/` — compile to Clojure

To run verify after compiling:
```bash
/home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ && clojure -Sdeps '{:paths [".build"]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
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
- Nominal scalar types: `ProductId`, `CategoryId`, `SupplierId`, `Price` (catalog); `CustomerId` (customers); `WarehouseId` (inventory); `OrderId`, `Timestamp`, `Amount` (orders); `ShipmentId`, `CarrierId`, `Weight` (shipping); `InvoiceId`, `PaymentId` (billing); `POId` (procurement); `CampaignId`, `CouponId` (promotions); `EmployeeId` (employees); `NotificationId`, `TemplateId` (notifications)
- Scalar constructors: `(->ProductId x)`, accessors: `(productid-value x)` — wrap/unwrap for arithmetic
- IDs are nominal scalars (not interchangeable)
- Timestamps are epoch **seconds** (not millis)
- Days = seconds / 86400
- Percentages: integer 0-100
- Tax rates are decimal (0.15 = 15%)
- Record accessors: `record-field` pattern (e.g., `carrier-base-rate`, `invoice-total`)
- Cross-module calls: `alias/function` (e.g., `ord/order-total`, `cat/product-unit-cost`)

## How to interpret checker output

Each error tells you exactly what's wrong:
- **E001 (arity)**: wrong number of arguments — the `sig:` line shows expected params
- **E002 (type mismatch)**: wrong type at a specific arg position — `help:` suggests the right accessor/function
- **`did you mean X?`** hints are almost always the correct fix
- Single suggestions → high confidence (beagle-fix auto-applies these)
- Multiple suggestions → you choose (checker lists all compatible alternatives)
- Missing args usually means a collection argument was forgotten

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

### Top-level forms
```racket
(def NAME : Type VALUE)
(defn NAME [PARAMS] : ReturnType BODY ...)
(defrecord Name [(field1 : Type) (field2 : Type)])
(defscalar Name BackingType)
```

### Expression forms
```racket
(if cond then else)
(cond [test1 body1] [test2 body2] [true fallback])
(let [name1 val1 name2 val2 ...] body...)
(fn [PARAMS] body...)
(for [x coll :when pred] body...)
```

### Types
Primitives: `String`, `Long`, `Double`, `Boolean`, `Keyword`, `Symbol`, `Nil`, `Any`
Function: `[A B -> R]`
Scalars: `ProductId`, `Amount`, `Timestamp`, etc. (backed by Long, incompatible with each other)

## Important

- Run `beagle-fix --apply .` FIRST — it auto-fixes mechanical type errors instantly
- Then run `beagle-check-all .` to see what remains
- After type errors are cleared, compile and run verify
- The verify script has 484 assertions — all must pass
- Use beagle-sig and beagle-fields to understand function contracts
