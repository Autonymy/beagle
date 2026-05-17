# Task: Fix bugs in a beagle trading system

You have a trading system with 6 modules written in beagle (a typed language that compiles to Clojure). The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes.

## Workflow

1. **Read the checker report below** — it shows all type errors and notes
2. **For each error/note**, read the relevant file and make the fix
3. **After fixing type errors**, run `beagle-check-all` once to verify no type errors remain
4. **Then read the verify script** to understand what behavioral correctness means
5. **Fix any remaining logic bugs** that the type checker cannot catch

## Checker report

The following errors and notes were found by `beagle-check-all`:

```
error[E001]: beagle: call to credit-account: expected 2 arg(s), got 3
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/accounts.rkt:55
   |
55 |         credited (credit-account to amt from)]
   |
   = sig: credit-account : [Account Amount -> Account]
   = help: extra argument(s): got 3, expected 2

error[E002]: beagle: with Instrument: field :ticker expected Ticker, got Price
   = note: Instrument fields of type Price: :price
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/instruments.rkt:67
   |
67 |   (with inst [:ticker new-price]))
   |

error[E002]: beagle: call to ->LedgerEntry: arg 2 expected AccountId, got Amount
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/ledger.rkt:56
   |
56 |                    (account-balance account)
   |
   = sig: ->LedgerEntry : [Long AccountId Long String Amount Amount Timestamp -> LedgerEntry]
   = note: account-balance : [Account -> Amount]
   = help: did you mean account-id? (account-id : [Account -> AccountId])

error[E002]: beagle: call to ->Order: arg 2 expected AccountId, got InstrumentId
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/orders.rkt:31
   |
31 |   (->Order (->OrderId id) instrument-id account-id
   |
   = sig: ->Order : [OrderId AccountId InstrumentId String Quantity Price String Timestamp Timestamp? -> Order]

error[E002]: beagle: call to ->PnLReport: arg 5 expected Timestamp, got String
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/reports.rkt:73
   |
73 |     (->PnLReport acct-id pnl fees n "now")))
   |
   = sig: ->PnLReport : [AccountId Amount Amount Long Timestamp -> PnLReport]

error[E001]: beagle: call to acct/format-balance: expected 1 arg(s), got 2
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/reports.rkt:102
    |
102 |         balance (acct/format-balance account "USD")
    |
   = sig: acct/format-balance : [Account -> String]
   = help: extra argument(s): got 2, expected 1

error[E002]: beagle: call to ->Trade: arg 1 expected TradeId, got OrderId
  --> /home/tom/code/beagle/experiments/e6-trading/trials/e6-beagle-run1/trades.rkt:35
   |
35 |     (->Trade (order-id order)
   |
   = sig: ->Trade : [TradeId OrderId AccountId InstrumentId String Quantity Price Amount Amount Timestamp -> Trade]
   = note: order-id : [Order -> OrderId]


6 file(s), 7 error(s)

```

## Key beagle concepts

- `defscalar Name BackingType` — creates a nominal type. `(defscalar Amount Long)` means Amount ≠ Long ≠ Timestamp even though all are Long underneath.
- `->Amount`, `->Timestamp` — scalar constructors (wrap a backing value)
- `amount-value`, `timestamp-value` — scalar accessors (unwrap to backing type)
- `(with record [:field value])` — typed record update
- Type errors mean the wrong scalar type is being used somewhere

## Module dependency order

```
accounts (leaf)
instruments (requires accounts)
orders (requires accounts, instruments)
trades (requires accounts, instruments, orders)
ledger (requires accounts, trades)
reports (requires all)
```

## Important

- Every `note:` line from the checker is an actionable hint — treat it as a bug to fix
- Scalar type errors (Amount vs Price vs Timestamp vs AccountId) are the most common bug class
- After fixing all type errors, look for logic bugs: wrong arithmetic, wrong comparisons, missing conditions
- The verify script is the final oracle — all its assertions must pass
