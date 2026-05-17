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

## Module dependency order

```
accounts (leaf)
instruments (requires accounts)
orders (requires accounts, instruments)
trades (requires accounts, instruments, orders)
ledger (requires accounts, trades)
reports (requires all)
```

## Domain semantics

- All monetary amounts are in cents (Long). $50.00 = 5000
- IDs are Long values
- Timestamps are epoch milliseconds
- Record accessors follow the pattern `record-field` (e.g., `account-balance`, `order-quantity`)
- Scalar comments (`;; Amount : Long (scalar)`) indicate domain types — these values should not be confused even though they're all Longs

## Important

- Read the verification script carefully — it defines correct behavior
- Pay attention to which value goes where: account-id vs order-id vs trade-id are all Longs but mean different things
- Watch for: wrong field access, swapped arguments, missing arithmetic operations, wrong comparisons
- The verify script is the final oracle — all its assertions must pass
