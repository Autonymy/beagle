# Task: Fix bugs in a 13-module Beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in Beagle. The code has bugs. Fix ALL bugs so the behavioral verification script passes (484 assertions).

## Beagle in 60 seconds

You know Clojure. Beagle is Clojure with static types. Files are `.rkt`
with `#lang beagle`. Compiles to plain `.clj`.

**Type annotations** on params and return types:
```racket
(defn total [(qty : Long) (price : Long)] : Long (* qty price))
```

**Records** generate typed constructors and accessors:
```racket
(defrecord Product [(id : Long) (name : String) (price : Long)])
;; (->Product 1 "Widget" 500)  — constructor [Long String Long -> Product]
;; (product-name p)            — accessor [Product -> String]
;; (:name p)                   — keyword access, inferred from record type
;; (with p [:price 600])       — typed update
```

**Nominal scalars** (newtypes): `(defscalar Amount Long)` — `(->Amount 500)` to wrap, `(amount-value a)` to unwrap. Amount ≠ Long at compile time.

**require** imports everything: `(require catalog :as cat)` — types, records, functions.

Types: `String`, `Long`, `Double`, `Boolean`, `Keyword`, `Nil`, `Any`, `(Vec T)`, `(Map K V)`, `(U A B)`, `String?` (= `(U String Nil)`)

## Reading checker errors → exact fixes

```
── E002 ── shipping.rkt:45 ──
  (carrier-base-rate zone)
  arg 1: expected Carrier, got DeliveryZone
  help: did you mean `zone-surcharge-pct`?
```
→ **"did you mean X?" is almost always correct. Use X.**

```
── E001 ── billing.rkt:61 ──
  (order-customer-id)
  called with 0 args, expects 1
  sig: order-customer-id : [Order -> Long]
```
→ Missing argument. Pass the Order.

**General rules:**
- E002 type mismatch → wrong accessor. Run `beagle-fields Record .` to see all fields.
- E001 arity → missing/extra arg. Run `beagle-sig fn .` for the signature.
- Single suggestion → high confidence, `beagle-fix` auto-applies these.
- Multiple suggestions → you choose from the listed alternatives.

## Workflow — follow this order exactly

### Step 0: Start the daemon (makes all queries 45× faster)
```bash
/home/tom/code/beagle/bin/beagle-daemon start
```

### Step 1: Apply the auto-patch
```bash
/home/tom/code/beagle/bin/beagle-repair . ../../verify/e8-full.verify.clj --emit-patch > /tmp/repair.patch 2>/dev/null && git apply /tmp/repair.patch 2>/dev/null
```
This auto-fixes ~6 mechanical type errors via a unified diff. Zero reasoning needed.

### Step 2: Auto-fix remaining mechanical errors
```bash
/home/tom/code/beagle/bin/beagle-fix --apply .
```

### Step 3: Check what remains
```bash
/home/tom/code/beagle/bin/beagle-check-all .
```
Fix remaining type errors manually using the checker output.

### Step 4: Validate paren balance after edits
```bash
/home/tom/code/beagle/bin/beagle-syntax *.rkt
```
Run this after editing to catch unmatched parens/brackets before compiling.

### Step 5: Compile and verify
```bash
/home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ && clojure -Sdeps '{:paths [".build"]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
```

### Step 6: Iterate until 484/484

## Available tools

All at `/home/tom/code/beagle/bin/`:

| Tool | What it does |
|------|-------------|
| `beagle-fix --apply .` | Auto-fix high-confidence type errors |
| `beagle-check-all .` | Type-check all files |
| `beagle-syntax *.rkt` | Fast paren/bracket balance check |
| `beagle-sig FN .` | Function signature |
| `beagle-fields RECORD .` | Record fields + accessor types |
| `beagle-callers FN .` | All call sites |
| `beagle-provides FILE` | Module exports |
| `beagle-impact FN .` | Downstream effects of a change |
| `beagle-repair . VERIFY` | Unified repair pipeline |
| `beagle-trace .build/ VERIFY` | Per-assertion arithmetic trace |
| `beagle-cascade . VERIFY --from-failures` | Root cause analysis |
| `beagle-build-all *.rkt --out .build/` | Compile to Clojure |

## Module dependency DAG

```
Layer 0 (leaves):     catalog, customers
Layer 1:              inventory (→ catalog)
                      orders (→ catalog, inventory, customers)
Layer 2:              reports (→ all layer 0–1)
Layer 3:              shipping, billing, procurement, promotions, employees
Layer 4:              analytics, notifications
Layer 5:              audit (→ all modules)
```

## Domain semantics

- Monetary amounts in **cents** (Long). $12.00 = 1200
- Nominal scalars: `ProductId`, `Amount`, `Timestamp`, `Price`, etc. — wrap/unwrap for arithmetic
- Timestamps = epoch **seconds**. Days = seconds / 86400
- Percentages: integer 0–100. Tax rates: decimal (0.15 = 15%)
- Record accessors: `record-field` pattern (e.g., `carrier-base-rate`)
- Cross-module: `alias/function` (e.g., `ord/order-total`, `cat/product-unit-cost`)

## Important

- Apply the patch and auto-fix FIRST — eliminates ~50% of type errors in zero turns
- Then check, fix remaining, compile, verify
- Use `beagle-syntax` after edits to catch paren corruption
- The verify script has 484 assertions — all must pass
