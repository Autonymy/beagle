# Task: Fix bugs in a 13-module beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow — patch-first repair

**Step 1: Generate and apply the repair patch**

```bash
/home/tom/code/beagle/bin/beagle-repair . ../../verify/e8-full.verify.clj --emit-patch > /tmp/repair.patch 2>&1
```

The patch file contains unified diffs for all high-confidence fixes. Apply it:

```bash
git apply /tmp/repair.patch
```

This applies ALL mechanical fixes (wrong accessor, swapped arguments, operand swap, value swap) in one shot. No need to read or reason about these — the repair toolchain has already verified each fix against the oracle.

**Step 2: Rebuild and verify**

```bash
/home/tom/code/beagle/bin/beagle-build-all --warn *.rkt --out .build/ && bb -cp .build -e '(load-file "../../verify/e8-full.verify.clj")'
```

**Step 3: Fix remaining failures**

The patch covers mechanical bugs. Semantic bugs (logic errors, wrong formulas) need your judgment. For these:

- `beagle-trace .build/ ../../verify/e8-full.verify.clj` — shows the exact operation that diverged
- `beagle-cascade . ../../verify/e8-full.verify.clj --from-failures` — finds root causes where one fix eliminates multiple failures
- `beagle-repair . ../../verify/e8-full.verify.clj` — human-readable queue with SUGGEST items

Fix highest cascade-score items first. Rebuild and re-verify after each fix.

## Available tools

All tools are at `/home/tom/code/beagle/bin/`:

### Repair pipeline
- `beagle-repair . VERIFY --emit-patch` — **unified diff output** (apply with `git apply`)
- `beagle-repair . VERIFY` — human-readable ranked queue (for SUGGEST items)
- `beagle-trace .build/ VERIFY [--focus FN]` — per-assertion arithmetic trace
- `beagle-cascade . VERIFY --from-failures` — root cause + cascade detection
- `beagle-specfix .build/ VERIFY` — oracle-verified candidate fixes
- `beagle-blame .build/ VERIFY` — quick ratio hints for triage

### Compile & check
- `beagle-build-all --warn *.rkt --out .build/` — compile (--warn emits despite type errors)
- `beagle-check-all .` — type-check all files

### Query
- `beagle-sig FN-NAME .` — function's type signature
- `beagle-fields RECORD .` — record fields + accessor types
- `beagle-callers FN-NAME .` — find all call sites
- `beagle-provides FILE` — list all exports with types
- `beagle-impact FN-NAME .` — callers + downstream impact

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
- Scalar constructors: `(->ProductId x)`, accessors: `(productid-value x)`
- Timestamps are epoch **seconds** (not millis)
- Percentages: integer 0-100
- Tax rates are decimal (0.15 = 15%)
- Record accessors: `record-field` pattern (e.g., `carrier-base-rate`, `invoice-total`)
- Cross-module calls: `alias/function` (e.g., `ord/order-total`, `cat/product-unit-cost`)

## How to interpret trace output

```
FAIL: ship/carrier-total-cost FastShip 10kg
  expected: 1500
  actual:   1001
  trace (last ops):
    (* 100 10) = 1000  ; shipping.rkt:44
    (+ 1 1000) = 1001  ; shipping.rkt:44
```

Read bottom-up: the last operation produced the actual value. Here `(+ 1 1000)` — that `1` is a carrier ID, not a base rate. Wrong accessor.
