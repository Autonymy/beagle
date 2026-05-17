# E6 Results: Trading System with Nominal Scalar Types

## Experiment Design

- **Domain**: 6-module trading system (accounts, instruments, orders, trades, ledger, reports)
- **Scale**: ~600 LOC per track, 40 injected bugs across all modules
- **Bug taxonomy**:
  - 7 scalar type confusion (beagle catches at compile time, Clojure silent)
  - 2 arity errors (both tracks catch at runtime)
  - 1 undefined function call (both catch at runtime)
  - 30 logic bugs (neither catches statically — wrong arithmetic, swapped fields, missing conditions)
- **Oracle**: behavioral verify script with 106 assertions, explicit per-bug test names
- **Agent**: Claude (sonnet via Agent tool), unlimited iterations, tools: Read/Edit/Bash
- **Beagle advantage**: pre-computed checker report (7 errors with signatures, hints, "did you mean?") injected into prompt

## Results

| Run | Track | Score | Time (s) | Tool Uses | Tokens |
|-----|-------|-------|-----------|-----------|--------|
| 1 | beagle | 106/106 | 292 | 45 | 50,842 |
| 2 | beagle | 106/106 | 165 | 44 | 44,612 |
| 3 | beagle | 106/106 | 195 | 43 | 48,317 |
| 1 | clojure | 106/106 | 178 | 44 | 45,066 |
| 2 | clojure | 106/106 | 180 | 44 | 44,979 |
| 3 | clojure | 106/106 | 268 | 43 | 47,032 |

### Averages

| Track | Score | Avg Time (s) | Avg Tools | Avg Tokens |
|-------|-------|--------------|-----------|------------|
| beagle | 106/106 (100%) | 217 | 44.0 | 47,924 |
| clojure | 106/106 (100%) | 209 | 43.7 | 45,692 |

## Analysis

### No correctness divergence

Both tracks achieved 100% on all 3 runs. The efficiency metrics (tokens, tool uses, wall time) are statistically indistinguishable.

### Why beagle didn't outperform

The behavioral verify script acted as a **complete runtime oracle** — equivalent in power to a type checker for the purpose of iterative bug-fixing:

1. **Explicit test names** (`bug-14-credit-account-adds-amount-not-timestamp`) told the agent exactly which function had which bug
2. **Assertion messages** (`"BUG-14: credit-account should add amount (5000) to balance"`) provided the same "did you mean?" guidance as beagle's type errors
3. **Unlimited iterations** meant the clojure agent could trial-and-error its way to correctness — the oracle caught every wrong fix

The beagle checker report gives errors **before** running code, but with an iterative oracle available, the clojure agent gets equivalent feedback after one `clj` invocation.

### Comparison to E4 (which DID show divergence)

E4 (8,570 LOC, 13 modules, 35 bugs) produced **correctness divergence**: beagle 3/3, clojure 0/3. Key differences:

| Factor | E4 | E6 |
|--------|----|----|
| Scale | 8,570 LOC / 13 modules | ~600 LOC / 6 modules |
| Bug density | 35 bugs across 484 assertions | 40 bugs across 106 assertions |
| Oracle explicitness | Generic assertion names | Per-bug labeled assertions |
| Context pressure | High (agents hit context limits) | Low (all completed easily) |
| Iteration cost | ~30s per verify cycle | ~5s per verify cycle |

### Hypothesis: where beagle's advantage materializes

Beagle outperforms when the agent **cannot iterate to convergence**:

1. **Scale**: more modules → higher iteration cost → context exhaustion before all bugs found
2. **Oracle incompleteness**: real-world tests don't have `BUG-N:` labels or 100% coverage
3. **Compound errors**: bug A masks bug B in test output, requiring reasoning about dependencies
4. **Token budgets**: limited agent context means type errors (free, upfront) vs. runtime errors (expensive, iterative) is a real tradeoff

## Conclusions

- **E6 validates the tooling**: both tracks work end-to-end, scoring is reliable, trial setup is automated
- **E6 does NOT show beagle advantage** at this scale with a complete oracle
- **E4 remains the definitive divergence result** (beagle 100%, clojure 0%)
- **Next experiment should**: (a) use a less explicit oracle (assertion messages without bug IDs), (b) impose a token budget, or (c) scale to 15+ modules where iteration cost dominates

## Compiler Fix (side effect)

During E6 setup, discovered and fixed a real compiler bug: **cross-module defscalar import + emit erasure**. Imported scalar constructors (`->Amount`, `->Currency`) and accessors (`amount-value`, `price-value`) from required modules were:
- Not registered as typed externs → type checker couldn't validate them
- Not erased in emit → compiled Clojure contained undefined symbols

Fix: `private/parse.rkt` now handles `defscalar` in `import-module-types!`, and `private/emit.rkt` erases imported scalar fns alongside local ones. 329 tests pass.
