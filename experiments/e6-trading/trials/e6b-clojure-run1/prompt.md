# Task: Fix bugs in a Clojure trading system

You have a trading system with 6 modules written in Clojure. The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes.

## Workflow

1. **Read the verify script** to understand what correctness means
2. **Run the verify script** to see which assertions fail
3. **Read the failing modules** and fix the bugs
4. **Re-run verify** until all assertions pass

## Available tools

- `clj-sig FUNCTION FILES...` — show a function's signature (parameter names)
- `clj-fields RECORD FILES...` — show record fields
- `clj-callers FUNCTION FILES...` — find all call sites
- `clj-provides FILES...` — list all exports from a module

## Domain semantics

- All monetary amounts are in **cents** (Long). $50.00 = 5000
- IDs are Long values
- Timestamps are epoch **milliseconds**
- Fees are in **basis points** (bps). 10 bps = 0.1% = multiply by 10/10000
- Days = milliseconds / 86400000 (not 3600000)
- Record accessors follow the pattern `:field-name` (keyword access on maps)
- Scalar comments (`;; Amount : Long (scalar)`) indicate domain types — these values should not be confused even though they're all Longs

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

- Read the verification script carefully — it defines correct behavior
- Pay attention to which value goes where: account-id vs order-id vs trade-id are all Longs but mean different things
- Watch for: wrong field access, swapped arguments, missing arithmetic operations, wrong comparisons
- The verify script is the final oracle — 106 assertions must pass
