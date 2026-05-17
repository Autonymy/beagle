# Task: Fix bugs in a 13-module beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow

1. **Read the checker report below** — it shows 20 compile-time errors with signatures and hints
2. **For each error**, read the relevant file, understand the context, and fix the bug
3. **After fixing type errors**, run `beagle-check-all .` to verify 0 errors remain
4. **Run the verify script** to find logic bugs the checker can't catch
5. **Iterate** fixing and re-verifying until all 484 assertions pass

## Checker report (20 errors)

```
error[E001]: beagle: call to ord/orders-in-period: expected 3 arg(s), got 2
  --> analytics.rkt:120
    |
120 |                 period-orders (ord/orders-in-period start end)
    |
   = sig: ord/orders-in-period : [Any Long Long -> Any]
   = help: missing: arg 3: Long

error[E002]: beagle: call to >: arg 2 expected Long, got String
  --> analytics.rkt:413
    |
413 |             [(> last-val first-val)
    |
   = sig: > : [Long Long -> Boolean]

error[E001]: beagle: call to inv/total-inventory-value: expected 2 arg(s), got 1
  --> analytics.rkt:641
    |
641 |         inv-val (inv/total-inventory-value levels)
    |
   = sig: inv/total-inventory-value : [Any Any -> Long]
   = help: missing: arg 2: Any

error[E002]: beagle: call to ->AuditEntry: arg 3 expected Long, got String
  --> audit.rkt:55
   |
55 |   (->AuditEntry id "shipment" (ship/shipment-status shipment)
   |
   = sig: ->AuditEntry : [Long String Long String Long Long String -> AuditEntry]
   = note: ship/shipment-status : [Shipment -> String]
   = help: did you mean ship/shipment-order-id? (ship/shipment-order-id : [Shipment -> Long])
   = help: did you mean ship/shipment-weight-kg? (ship/shipment-weight-kg : [Shipment -> Long])
   = help: did you mean ship/shipment-id? (ship/shipment-id : [Shipment -> Long])

error[E001]: beagle: call to ord/orders-in-period: expected 3 arg(s), got 2
  --> audit.rkt:243
    |
243 |   (let [period-orders (ord/orders-in-period orders period-start)
    |
   = sig: ord/orders-in-period : [Any Long Long -> Any]
   = help: missing: arg 3: Long

error[E001]: beagle: call to ord/find-order-by-id: expected 2 arg(s), got 1
  --> audit.rkt:358
    |
358 |   (let [order (ord/find-order-by-id (invoice-order-id invoice))
    |
   = sig: ord/find-order-by-id : [Any Long -> Any]
   = help: missing: arg 2: Long

error[E001]: beagle: call to cust/customer-id: expected 1 arg(s), got 0
  --> billing.rkt:37
   |
37 |         cid   (cust/customer-id)
   |
   = sig: cust/customer-id : [Customer -> Long]
   = help: missing: arg 1: Customer

error[E002]: beagle: call to ->Invoice: arg 11 expected Long, got String
  --> billing.rkt:68
   |
68 |              (invoice-due-at inv) (invoice-status inv)))
   |
   = sig: ->Invoice : [Long Long Long Long Long Long Long String Long Long Long -> Invoice]
   = note: invoice-status : [Invoice -> String]
   = help: did you mean invoice-due-at? (invoice-due-at : [Invoice -> Long])
   = help: did you mean invoice-total? (invoice-total : [Invoice -> Long])
   = help: did you mean invoice-paid-at? (invoice-paid-at : [Invoice -> Long])

error[E001]: beagle: call to cat/find-product-by-id: expected 2 arg(s), got 1
  --> employees.rkt:67
   |
67 |                     product (cat/find-product-by-id prod-id)]
   |
   = sig: cat/find-product-by-id : [Any Long -> Any]
   = help: missing: arg 2: Long

error[E002]: beagle: call to total-commission: arg 2 expected Long, got String
  --> employees.rkt:86
   |
86 |                         curr-comm (total-commission rules (employee-name e) orders products)]
   |
   = sig: total-commission : [Any Long Any Any -> Long]
   = note: employee-name : [Employee -> String]
   = help: did you mean employee-id? (employee-id : [Employee -> Long])
   = help: did you mean employee-hire-date? (employee-hire-date : [Employee -> Long])

error[E001]: beagle: call to ->Notification: expected 8 arg(s), got 7
  --> notifications.rkt:221
    |
221 |   (->Notification id
    |
   = sig: ->Notification : [Long Long Long Long String String Long Long -> Notification]
   = help: missing: arg 8: Long

error[E002]: beagle: call to ->Notification: arg 4 expected Long, got String
  --> notifications.rkt:237
    |
237 |                   (ship/shipment-status shipment)
    |
   = sig: ->Notification : [Long Long Long Long String String Long Long -> Notification]
   = note: ship/shipment-status : [Shipment -> String]
   = help: did you mean ship/shipment-order-id? (ship/shipment-order-id : [Shipment -> Long])
   = help: did you mean ship/shipment-weight-kg? (ship/shipment-weight-kg : [Shipment -> Long])
   = help: did you mean ship/shipment-id? (ship/shipment-id : [Shipment -> Long])

error[E002]: beagle: call to ->PurchaseOrder: arg 2 expected Long, got String
  --> procurement.rkt:79
   |
79 |     (->PurchaseOrder id "pending" "pending" created-at expected-at total)))
   |
   = sig: ->PurchaseOrder : [Long Long String Long Long Long -> PurchaseOrder]

error[E001]: beagle: call to inv/reorder-needed: expected 1 arg(s), got 0
  --> procurement.rkt:344
    |
344 |   (let [needed (inv/reorder-needed)]
    |
   = sig: inv/reorder-needed : [Any -> Any]
   = help: missing: arg 1: Any

error[E002]: beagle: call to <=: arg 1 expected Long, got String
  --> promotions.rkt:37
   |
37 |        (<= (campaign-name campaign) now)
   |
   = sig: <= : [Long Long -> Boolean]
   = note: campaign-name : [Campaign -> String]
   = help: did you mean campaign-id? (campaign-id : [Campaign -> Long])
   = help: did you mean campaign-end-date? (campaign-end-date : [Campaign -> Long])
   = help: did you mean campaign-start-date? (campaign-start-date : [Campaign -> Long])

error[E001]: beagle: call to coupon-discount-value: expected 1 arg(s), got 0
  --> promotions.rkt:110
    |
110 |                 (coupon-discount-value))]
    |
   = sig: coupon-discount-value : [Coupon -> Long]
   = help: missing: arg 1: Coupon

error[E002]: beagle: call to tier-qualifies?: arg 1 expected String, got Long
  --> promotions.rkt:156
    |
156 |   (tier-qualifies? (cust/customer-id customer)
    |
   = sig: tier-qualifies? : [String String -> Boolean]
   = note: cust/customer-id : [Customer -> Long]
   = help: did you mean cust/customer-name? (cust/customer-name : [Customer -> String])
   = help: did you mean cust/customer-tier? (cust/customer-tier : [Customer -> String])
   = help: did you mean cust/customer-email? (cust/customer-email : [Customer -> String])

error[E001]: beagle: call to carrier-base-rate: expected 1 arg(s), got 0
  --> shipping.rkt:37
   |
37 |   (- (carrier-base-rate) (* (carrier-per-kg-rate c) weight-kg)))
   |
   = sig: carrier-base-rate : [Carrier -> Long]
   = help: missing: arg 1: Carrier

error[E001]: beagle: call to zone-surcharge: expected 2 arg(s), got 1
  --> shipping.rkt:71
   |
71 |         surcharge (zone-surcharge base-cost)
   |
   = sig: zone-surcharge : [DeliveryZone Long -> Long]
   = help: missing: arg 2: Long

error[E002]: beagle: call to shipments-for-order: arg 2 expected Long, got String
  --> shipping.rkt:203
    |
203 |   (let [order-shipments (shipments-for-order shipments "pending")]
    |
   = sig: shipments-for-order : [Any Long -> Any]

13 file(s), 20 error(s)
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
Layer 4:              analytics (→ orders, inventory, billing, shipping)
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

## Available tools

All tools are at `/home/tom/code/beagle/bin/`:

- `/home/tom/code/beagle/bin/beagle-check-all .` — re-run checker on all 13 files
- `/home/tom/code/beagle/bin/beagle-sig FN-NAME .` — print function's type signature
- `/home/tom/code/beagle/bin/beagle-fields RECORD .` — print record fields + accessor types
- `/home/tom/code/beagle/bin/beagle-callers FN-NAME .` — find all call sites
- `/home/tom/code/beagle/bin/beagle-provides FILE` — list all exports with types
- `/home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/` — compile all to Clojure

To run verify after compiling:
```bash
/home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ && clojure -Sdeps '{:paths [".build"]}' -M e4.verify.clj
```

## How to interpret checker output

Each error tells you exactly what's wrong:
- **E001 (arity)**: wrong number of arguments — the `sig:` line shows expected params
- **E002 (type mismatch)**: wrong type at a specific arg position — `help:` suggests the right accessor/function
- **`did you mean X?`** hints are almost always the correct fix
- Missing args usually means a collection argument was forgotten (e.g., `orders` or `products`)

## beagle language reference

```racket
#lang beagle

(ns my.namespace)
(define-mode strict)
(require some.module :as mod)
(declare-extern fn [Args -> Ret])  ; ONLY for Java interop / non-beagle fns
```

`(require module :as alias)` imports all typed defs, defns, records, and macros.

### Top-level forms

```racket
(def NAME : Type VALUE)
(defn NAME [PARAMS] : ReturnType BODY ...)
(defrecord Name [(field1 : Type) (field2 : Type)])
(unsafe "raw clojure source")
```

### `defrecord`

```racket
(defrecord Employee [(name : String) (rate : Long)])
```

Generated: constructor `(->Employee "Alice" 95)`, accessors `(employee-name e)`, `(employee-rate e)`.

### Expression forms

```racket
(if cond then else)
(cond [test1 body1] [test2 body2] [true fallback])
(when cond body...)
(do body1 body2 ... bodyN)
(let [name1 val1 name2 val2 ...] body...)
(loop [name1 init1 ...] body...)
(recur arg1 arg2 ...)
(for [x coll :when pred] body...)
(fn [PARAMS] body...)
(try body... (catch ExType e handler...) (finally cleanup...))
(match expr [(RecordType f1 f2) body...] [_ default...])
(-> x (f) (g))               ; thread-first
(->> x (f) (g))              ; thread-last
(:key map)                   ; keyword-as-function
```

### Types

Primitives: `String`, `Long`, `Double`, `Boolean`, `Keyword`, `Symbol`, `Nil`, `Any`
Function: `[A B -> R]`, `[A & T -> R]` (variadic)
Parametric: `(Vec T)`, `(List T)`, `(Set T)`, `(Map K V)`
Union: `(U String Long)`, nullable: `String?` = `(U String Nil)`

### Pre-typed stdlib (~607 functions)

Math: `+`, `-`, `*`, `/`, `mod`, `quot`, `rem`, `inc`, `dec`, `min`, `max`, `abs`
Comparison: `=`, `not=`, `<`, `>`, `<=`, `>=`, `zero?`, `pos?`, `neg?`
Boolean: `not`, `nil?`, `some?`
Collections: `first`, `rest`, `nth`, `count`, `conj`, `into`, `assoc`, `dissoc`, `update`, `merge`, `get`, `contains?`, `keys`, `vals`, `vec`, `sort`, `sort-by`, `group-by`
Higher-order: `map`, `mapv`, `filter`, `filterv`, `remove`, `reduce`, `apply`, `every?`, `some`
Strings: `str`, `name`, `subs`

## Important

- Fix all 20 type errors FIRST — they are guaranteed bugs with exact location + fix hint
- After type errors are cleared, run verify to find remaining logic bugs
- The verify script has 484 assertions — all must pass
- Read the verify script to understand what each function should produce
- This is a large codebase — use beagle-sig and beagle-fields to understand function contracts without reading entire files
