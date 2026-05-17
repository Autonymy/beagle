# Task: Fix bugs in a beagle trading system

You have a trading system with 6 modules written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes.

## Workflow

1. **Read the checker report below** — it shows all type errors and provenance notes
2. **For each error/note**, read the relevant file and make the fix
3. **After fixing type errors**, run `beagle-check-all .` to verify no type errors remain
4. **Run the verify script** to see which behavioral assertions still fail
5. **Fix any remaining logic bugs** that the type checker cannot catch
6. **Iterate** until verify reports 106/106 assertions passing

## Checker report

The following errors and notes were found by `beagle-check-all`:

```
error[E001]: beagle: call to credit-account: expected 2 arg(s), got 3
  --> accounts.rkt:55
   |
55 |         credited (credit-account to amt from)]
   |
   = sig: credit-account : [Account Amount -> Account]
   = help: extra argument(s): got 3, expected 2

note: unused parameter 'amt' in credit-account
  --> accounts.rkt:37
note: scalar provenance: ->Amount receives value derived from Timestamp
  --> accounts.rkt:39
  = Amount wraps a Long backing value, but the argument originated from Timestamp
note: mixed scalar provenance in arithmetic: Amount, Timestamp used together
  --> accounts.rkt:39

error[E002]: beagle: with Instrument: field :ticker expected Ticker, got Price
   = note: Instrument fields of type Price: :price
  --> instruments.rkt:67
   |
67 |   (with inst [:ticker new-price]))
   |

note: unused let binding 'q'
  --> instruments.rkt:38
note: scalar provenance: ->Amount receives value derived from Price
  --> instruments.rkt:39
  = Amount wraps a Long backing value, but the argument originated from Price

error[E002]: beagle: call to ->LedgerEntry: arg 2 expected AccountId, got Amount
  --> ledger.rkt:56
   |
56 |                    (account-balance account)
   |
   = sig: ->LedgerEntry : [Long AccountId Long String Amount Amount Timestamp -> LedgerEntry]
   = note: account-balance : [Account -> Amount]
   = help: did you mean account-id? (account-id : [Account -> AccountId])

error[E002]: beagle: call to ->Order: arg 2 expected AccountId, got InstrumentId
  --> orders.rkt:31
   |
31 |   (->Order (->OrderId id) instrument-id account-id
   |
   = sig: ->Order : [OrderId AccountId InstrumentId String Quantity Price String Timestamp Timestamp? -> Order]

note: unused let binding 'q'
  --> orders.rkt:39
note: scalar provenance: ->Amount receives value derived from Price
  --> orders.rkt:40
  = Amount wraps a Long backing value, but the argument originated from Price
note: scalar provenance: ->Amount receives value derived from OrderId
  --> orders.rkt:45
  = Amount wraps a Long backing value, but the argument originated from OrderId
note: unused parameter 'fill-ts' in fill-order
  --> orders.rkt:54
note: cross-scalar comparison: instrumentid vs accountid
  --> orders.rkt:67
  = comparing values derived from incompatible scalar types
note: scalar provenance: ->Amount receives value derived from OrderId
  --> orders.rkt:79
  = Amount wraps a Long backing value, but the argument originated from OrderId

error[E002]: beagle: call to ->PnLReport: arg 5 expected Timestamp, got String
  --> reports.rkt:73
   |
73 |     (->PnLReport acct-id pnl fees n "now")))
   |
   = sig: ->PnLReport : [AccountId Amount Amount Long Timestamp -> PnLReport]

error[E001]: beagle: call to acct/format-balance: expected 1 arg(s), got 2
  --> reports.rkt:102
    |
102 |         balance (acct/format-balance account "USD")
    |
   = sig: acct/format-balance : [Account -> String]
   = help: extra argument(s): got 2, expected 1

note: scalar provenance: ->Amount receives value derived from Price
  --> reports.rkt:48
  = Amount wraps a Long backing value, but the argument originated from Price
note: scalar provenance: ->Amount receives value derived from Price
  --> reports.rkt:61
  = Amount wraps a Long backing value, but the argument originated from Price
note: unused parameter 'ts' in generate-pnl-report
  --> reports.rkt:66
note: unused let binding 'pnl-val'
  --> reports.rkt:87
note: call to undefined function 'calculate-pnl'
  --> reports.rkt:89

error[E002]: beagle: call to ->Trade: arg 1 expected TradeId, got OrderId
  --> trades.rkt:35
   |
35 |     (->Trade (order-id order)
   |
   = sig: ->Trade : [TradeId OrderId AccountId InstrumentId String Quantity Price Amount Amount Timestamp -> Trade]
   = note: order-id : [Order -> OrderId]

note: unused let binding 'acct-id'
  --> trades.rkt:63
note: cross-scalar comparison: instrumentid vs accountid
  --> trades.rkt:72
  = comparing values derived from incompatible scalar types

7 error(s), 17 note(s)
```

## Domain semantics

- All monetary amounts are in **cents** (Long). $50.00 = 5000
- IDs are Long values (but wrapped in scalars: AccountId, OrderId, TradeId, InstrumentId)
- Timestamps are epoch **milliseconds**
- Fees are in **basis points** (bps). 10 bps = 0.1% = multiply by 10/10000
- Days = milliseconds / 86400000 (not 3600000)
- Record accessors: `account-balance`, `order-quantity`, `trade-exec-price`, etc.

## Module dependency order

```
accounts (leaf)
instruments (requires accounts)
orders (requires accounts, instruments)
trades (requires accounts, instruments, orders)
ledger (requires accounts, trades)
reports (requires all)
```

## Available tools

- `beagle-check-all .` — re-run checker on all files
- `beagle-sig FN-NAME .` — print function's type signature
- `beagle-fields RECORD .` — print record fields + accessor types
- `beagle-callers FN-NAME .` — find all call sites
- `beagle-provides FILE` — list all exports with types

## How to interpret checker output

- **Errors** are definite type violations — wrong scalar passed to constructor, wrong arity
- **Notes** are high-confidence bug indicators:
  - `scalar provenance: ->X receives value derived from Y` → you're wrapping the wrong backing value
  - `cross-scalar comparison` → comparing IDs of different entity types
  - `unused parameter` → the function should be using that parameter but isn't
  - `unused let binding` → computed value is never used (the real logic uses the wrong variable)
  - `call to undefined function` → typo or the function doesn't exist (use the right name)

Every note corresponds to a real bug. Fix them all.

## beagle language reference

```racket
#lang beagle

(ns my.namespace)
(define-mode strict)
(require some.module :as mod)
(declare-extern fn [Args -> Ret])  ; ONLY for Java interop / non-beagle fns
(import java.io.File)
```

`(require module :as alias)` imports all typed defs, defns, records, scalars, and macros.

### Top-level forms

```racket
(def NAME VALUE)
(def NAME : Type VALUE)

(defn NAME [PARAMS] BODY ...)
(defn NAME [PARAMS] : ReturnType BODY ...)

(defrecord Name [(field1 : Type) (field2 : Type)])
(defscalar Amount Long)           ; nominal type backed by Long

(define-macro safe NAME (params) template)
(define-macro unsafe NAME (params) template)

(unsafe "raw clojure source")     ; emits verbatim
```

### `defscalar` (nominal types)

```racket
(defscalar Amount Long)
(defscalar Timestamp Long)
(defscalar AccountId Long)
```

Creates nominal types backed by primitives. Amount, Timestamp, and AccountId are all Long at runtime but incompatible at compile time.

Generated:
- Constructor: `(->Amount 5000)` — typed `[Long -> Amount]`
- Accessor: `(amount-value x)` — typed `[Amount -> Long]`

### `defrecord`

```racket
(defrecord Employee [(name : String) (rate : Long)])
```

Generated:
- Constructor: `(->Employee "Alice" 95)` — typed `[String Long -> Employee]`
- Accessors: `(employee-name e)`, `(employee-rate e)`

### `with` (record update)

```racket
(with record [:field1 new-val] [:field2 new-val2])
```

Typed record update → `(assoc ...)`. Validates field exists and value type matches.

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
(-> x (f) (g))                ; thread-first
(->> x (f) (g))               ; thread-last
(:key map)                    ; keyword-as-function
```

### Parameter syntax

```racket
[(x : Long) (y : String)]     ; typed params (canonical)
[x y z]                        ; untyped (type inferred)
[{:keys [name age]}]           ; map destructuring
[[a b & rest]]                 ; sequential destructuring
```

### Types

Primitives: `String`, `Long`, `Double`, `Boolean`, `Keyword`, `Symbol`, `Nil`, `Any`
Function: `[A B -> R]`, `[A & T -> R]` (variadic)
Parametric: `(Vec T)`, `(List T)`, `(Set T)`, `(Map K V)`
Union: `(U String Long)`, nullable sugar: `String?` = `(U String Nil)`

### Pre-typed stdlib (~607 functions)

Math: `+`, `-`, `*`, `/`, `mod`, `inc`, `dec`, `min`, `max`, `abs`
Comparison: `=`, `not=`, `<`, `>`, `<=`, `>=`, `zero?`, `pos?`, `neg?`
Boolean: `not`, `and`, `or`, `nil?`, `some?`
Collections: `first`, `rest`, `nth`, `count`, `conj`, `into`, `assoc`, `dissoc`, `update`, `merge`, `get`, `contains?`, `keys`, `vals`, `vec`, `sort`
Higher-order: `map`, `mapv`, `filter`, `filterv`, `remove`, `reduce`, `apply`, `comp`, `partial`, `every?`, `some`
Strings: `str`, `name`, `subs`
IO: `println`, `print`
Errors: `throw`, `ex-info`

## Important

- Every error AND every note from the checker is an actionable bug
- After fixing checker issues, run verify to find remaining logic bugs
- The verify script is the final oracle — 106 assertions must pass
