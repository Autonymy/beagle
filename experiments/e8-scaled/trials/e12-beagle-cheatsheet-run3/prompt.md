# Task: Fix bugs in a 13-module Beagle inventory system

You have a large inventory & order management system (13 modules, ~8500 LOC) written in Beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Beagle language reference

Beagle is a typed language that compiles to Clojure. This reference covers
everything needed to write beagle code. Errors are caught at compile time.

### File header

```racket
#lang beagle

(ns my.namespace)
(define-mode strict)                   ; default; or `dynamic` to skip checks
(require other.module :as mod)         ; imports all types/fns from beagle module
(declare-extern fn-name [Args -> Ret]) ; ONLY for Java interop / non-beagle fns
(import java.io.File)                  ; Java class import
```

`(require module :as alias)` imports everything — types, records, functions,
macros. You do NOT need `declare-extern` for cross-module beagle calls.

### Definitions

```racket
(def NAME VALUE)
(def NAME : Type VALUE)

(defn NAME [PARAMS] BODY ...)
(defn NAME [PARAMS] : ReturnType BODY ...)

(defrecord Name [(field1 : Type) (field2 : Type)])

(defscalar Amount Long)                ; nominal type (Amount ≠ Long at compile time)
;; Constructor: (->Amount 5000) typed [Long -> Amount]
;; Accessor: (amount-value x) typed [Amount -> Long]
;; Arithmetic: (+ (amount-value a) (amount-value b))
;; Rewrap: (->Amount result)

(defenum Status :active :inactive)     ; enum value set
```

### Records

```racket
(defrecord Employee [(name : String) (rate : Long)])
```

Generates:
- Constructor: `(->Employee "Alice" 95)` — typed `[String Long -> Employee]`
- Accessors: `(employee-name e)`, `(employee-rate e)`
- Keyword access: `(:name e)` returns `String` when `e` is `Employee`
- Update: `(with e [:rate 100])` — compile-time field + type validation

### Expressions

```racket
(if cond then else)
(cond [test1 body1] [test2 body2] [true fallback])
(when cond body...)
(do body1 body2 ... bodyN)
(let [name1 value1 name2 value2 ...] body...)
(loop [name1 init1 ...] body...)
(recur arg1 arg2 ...)
(for [x coll :when pred] body...)      ; returns (Vec BodyType)
(doseq [x coll] body...)              ; side-effecting, returns nil
(fn [PARAMS] body...)
(try body... (catch ExType e handler...) (finally cleanup...))
(case test val1 result1 val2 result2 default)
(match expr [pattern body...] ...)
(-> x (f) (g))                        ; thread-first
(->> x (f) (g))                       ; thread-last
(:key map)                            ; keyword lookup
[item1 item2 ...]                     ; vector literal
{k1 v1 k2 v2}                        ; map literal
#{item1 item2}                        ; set literal
(unsafe "raw clojure")                ; escape hatch (typed Any)
```

### Parameters

```racket
[(x : Long) (y : String)]             ; typed (canonical form)
[x y z]                               ; untyped
[{:keys [name age]}]                  ; map destructuring
[{:keys [x y] :as point}]            ; destructure + bind whole
[[a b & rest]]                        ; sequential destructuring
```

### Types

| Type | Matches |
|------|---------|
| `String` | strings |
| `Long` | integers |
| `Double` | floats |
| `Boolean` | true/false |
| `Keyword` | `:foo` |
| `Nil` | `nil` |
| `Any` | anything (escape) |

```
[A B -> R]            ; function type
[A & T -> R]          ; variadic
(Vec T)               ; vector of T
(Map K V)             ; map
(Set T)               ; set
(U String Long)       ; union
String?               ; nullable (= (U String Nil))
```

### What the checker catches

- Wrong type passed to function or constructor
- Wrong number of arguments (reports available arities)
- Wrong record field in `with` update
- Collection element type mismatches (`(Vec Product)` vs `(Vec Customer)`)
- Destructured field type mismatches
- Nullable return when non-nullable declared
- Undefined function references
- Cross-module contract violations

### Let bindings infer types

```racket
(let [x (get-product id)] ...)     ; x : Product (inferred from return type)
(let [{:keys [name]} product] ...) ; name : String (inferred from record fields)
```

### Pattern matching

```racket
(match expr
  [(RecordType f1 f2) body...]    ; type test + positional destructure
  [{:key1 p1} body...]            ; map pattern
  [nil body...]                   ; literal
  [var body...]                   ; bind
  [_ body...])                    ; wildcard
```

### Multi-arity

```racket
(defn greet
  ([(name : String)] : String (str "Hello, " name))
  ([(name : String) (title : String)] : String (str "Hello, " title " " name)))
```

---

## Workflow — IMPORTANT: follow this order

1. **Run `beagle-fix --apply .`** — auto-fixes high-confidence type errors (accessor swaps, single-candidate fixes)
2. **Run `beagle-check-all .`** — see remaining type errors and fix them manually. The checker output tells you exactly what's wrong.
3. **After type errors are cleared**, compile and run verify:
   ```bash
   /home/tom/code/beagle/bin/beagle-build-all *.rkt --out .build/ && clojure -Sdeps '{:paths [".build"]}' -M -e '(load-file "../../verify/e8-full.verify.clj")'
   ```
4. **Iterate** fixing and re-verifying until all 484 assertions pass

## Available tools

All tools are at `/home/tom/code/beagle/bin/`:

### Repair tools (use these first)
- `beagle-fix --apply .` — auto-fix high-confidence type errors
- `beagle-fix --dry-run .` — preview what would be fixed
- `beagle-check-all .` — type-check all 13 files
- `beagle-repair . ../../verify/e8-full.verify.clj` — unified repair pipeline: type errors + blame + specfix → ranked repair queue
- `beagle-trace .build/ ../../verify/e8-full.verify.clj` — per-assertion arithmetic trace showing exact divergence point
- `beagle-cascade . ../../verify/e8-full.verify.clj --from-failures` — call graph impact: find root causes, not symptoms
- `beagle-blame .build/ ../../verify/e8-full.verify.clj` — ratio analysis: sign error, wrong operator, missing term

### Query tools
- `beagle-sig FN-NAME .` — print function's type signature
- `beagle-fields RECORD .` — print record fields + accessor types
- `beagle-callers FN-NAME .` — find all call sites
- `beagle-provides FILE` — list all exports with types
- `beagle-impact FN-NAME .` — show callers and downstream effects of a signature change

### Build
- `beagle-build-all *.rkt --out .build/` — compile to Clojure

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

## How to interpret verify failures

```
FAIL: cat/product-margin Widget A
  expected: 700
  actual:   -700
```
→ `product-margin` returns -700 when it should return 700. Read the function, understand the computation, fix the logic.

Common bug patterns:
- Wrong operator (+ instead of *, - instead of +)
- Swapped operands (a - b should be b - a)
- Wrong accessor (using `product-id` where `product-unit-cost` needed)
- Wrong argument passed to function
- Comparison direction (> should be <)
- Wrong divisor (10 instead of 100 for percentages)
- Missing or extra arguments (wrong arity)

## Important

- Run `beagle-fix --apply .` FIRST — it auto-fixes mechanical type errors instantly
- Then run `beagle-check-all .` to see what remains
- Use `beagle-repair` for a unified view of type errors + behavioral failures
- After type errors are cleared, compile and run verify
- The verify script has 484 assertions — all must pass
- Use beagle-sig and beagle-fields to understand function contracts
