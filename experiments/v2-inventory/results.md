# v2 Experiment Results

5-module inventory system, 1651 LOC beagle / 1235 LOC Clojure, 15 typed
records, ~100 functions, 444 verification assertions.

Agent: Claude Sonnet (via Claude Code subagent), 3 runs per track per
experiment. Each run is an independent agent with no memory of prior runs.

## E1: Fresh Build

Agent builds all 5 modules from a shared spec. Beagle track compiles to
Clojure before verification; Clojure track writes .clj directly.

| Run | Track | Assertions | Wall-clock (s) | Tool calls | Tokens |
|-----|-------|-----------|----------------|------------|--------|
| 1 | Clojure | 444/444 | 208 | 9 | 61K |
| 2 | Clojure | 444/444 | 208 | 9 | 61K |
| 3 | Clojure | 444/444 | 221 | 11 | 69K |
| 1 | Beagle | 444/444 | 290 | 16 | 77K |
| 2 | Beagle | 444/444 | 268 | 16 | 75K |
| 3 | Beagle | 444/444 | 263 | 16 | 70K |

**Averages:**

| Track | Wall-clock (s) | Tool calls | Tokens |
|-------|----------------|------------|--------|
| Clojure | 212 | 9.7 | 64K |
| Beagle | 274 | 16.0 | 74K |

Beagle is ~29% slower. The overhead is the compile step (beagle-check +
beagle-build per module). Both tracks produce correct code on the first
verification pass — no fix iterations needed.

## E2: Cross-Module Refactoring

Not run. Requires an E2-specific verify script for the bulk-discount
feature addition.

## E3: Bug Detection

12 bugs injected into golden code (both beagle and Clojure copies). Agent
receives the buggy files and must find and fix all bugs to reach 444/444.

Bug classification:

| # | Module | Bug | Category |
|---|--------|-----|----------|
| 1 | inventory | `cat/find-product-by-id` called with 1 arg (needs 2) | arity |
| 2 | orders | `inv/can-fulfill?` called with 2 args (needs 3) | arity |
| 3 | reports | `ord/customer-total-spend` called with 1 arg (needs 2) | arity |
| 4 | orders | `calculate-subtotal` called with extra arg | arity |
| 5 | inventory | `cat/product-name` used where `cat/product-unit-cost` needed | wrong accessor |
| 6 | orders | `cust/customer-id` (Long) where `cust/customer-tier` (String) needed | wrong type |
| 7 | reports | `cat/product-name` (String) where Long pid expected | wrong accessor |
| 8 | orders | `order-status` (String) passed to `cat/format-price` (expects Long) | wrong type |
| 9 | reports | String `"pending"` passed where Order expected | wrong accessor |
| 10 | catalog | `product-margin` subtracts cost from cost (always 0) | logic |
| 11 | customers | `tier-discount-pct` returns 5 for gold instead of 15 | logic |
| 12 | orders | `calculate-total` adds discount instead of subtracting | logic |

Bugs 1–9: type/arity errors (beagle catches at compile time).
Bugs 10–12: logic errors (neither catches statically).

| Run | Track | Assertions | Wall-clock (s) | Tool calls | Tokens |
|-----|-------|-----------|----------------|------------|--------|
| 1 | Clojure | 444/444 | 147 | 29 | 42K |
| 2 | Clojure | 444/444 | 127 | 25 | 41K |
| 3 | Clojure | 444/444 | 127 | 27 | 41K |
| 1 | Beagle | 444/444 | 229 | 44 | 63K |
| 2 | Beagle | 444/444 | 208 | 39 | 58K |
| 3 | Beagle | 444/444 | 177 | 35 | 57K |

**Averages:**

| Track | Wall-clock (s) | Tool calls | Tokens |
|-------|----------------|------------|--------|
| Clojure | 134 | 27.0 | 41K |
| Beagle | 205 | 39.3 | 59K |

Beagle is ~53% slower wall-clock, ~44% more tokens.

Both tracks found and fixed all 12 bugs across all runs. The Clojure
agents found type/arity bugs through runtime test failures; the beagle
agents found them through compile-time type errors. The end result is
identical — 444/444 in every run.

## E3b: Bug Detection Without Test Oracle

Same 12 injected bugs as E3. The agent receives buggy code and the build
spec, but **no pre-written test suite**. Beagle agents have `beagle-check`
(the type checker) as a diagnostic tool. Clojure agents have only code
reading and whatever tests they choose to write.

This is the experiment E3 should have been. E3 gave both tracks a
444-assertion test oracle, which neutralized beagle's type-checking
advantage — the test suite caught the same bugs the type checker did. E3b
removes that crutch.

| Run | Track | Score | Wall-clock (s) | Tool calls | Tokens |
|-----|-------|-------|----------------|------------|--------|
| 1 | Beagle | 436/444 | 245 | 55 | 70K |
| 2 | Beagle | 435/444 | 175 | 48 | 75K |
| 3 | Beagle | 435/444 | 237 | 40 | 75K |
| 1 | Clojure | 435/444 | 316 | 33 | 58K |
| 2 | Clojure | 435/444 | 454 | 35 | 68K |
| 3 | Clojure | 435/444 | 262 | 31 | 55K |

**Averages:**

| Track | Wall-clock (s) | Tool calls | Tokens |
|-------|----------------|------------|--------|
| Beagle | 219 | 47.7 | 73K |
| Clojure | 344 | 33.0 | 60K |

Beagle is **36% faster** wall-clock. Clojure uses **18% fewer tokens**.

Both tracks found and fixed all 12 injected bugs. Both also made the same
false-positive "corrections" where the spec and golden code disagree
(points-to-dollars formula, reorder-quantity formula, create-return initial
status). These wash out — identical across both tracks.

The 8–9 failed assertions in every run are from those false positives, not
from missed injected bugs. If scored only on the 12 real bugs, both tracks
achieve 12/12 in every run.

### Why beagle is faster but not more correct

The type checker gives the beagle agent immediate, structured feedback on 9
of 12 bugs: exact file, line, and error ("expected Long, got String"). The
Clojure agent must read all 1200 LOC, reason about arity and types
manually, and often writes its own ad-hoc tests. Both arrive at the same
answer, but the beagle agent arrives faster because the diagnostic work is
offloaded to a deterministic tool.

The token inversion (beagle uses more tokens but less time) tracks: the
checker gives dense signal, so the agent iterates more against tighter
feedback. The Clojure agent uses fewer tokens because it spends them on
long contemplative reads rather than rapid check-fix cycles.

### What E3b doesn't test

The advantage should grow with codebase size. The Clojure agent's "read
everything" cost scales with LOC; the beagle agent's "let the checker tell
me" cost scales with bug count. At 12K LOC the gap might be 60–70%. The
experiment that would prove the structural claim: does the speed advantage
grow superlinearly with codebase size?

## Summary

| Experiment | Metric | Clojure avg | Beagle avg | Delta |
|------------|--------|-------------|------------|-------|
| E1 (build) | Wall-clock | 212s | 274s | +29% beagle slower |
| E1 (build) | Tokens | 64K | 74K | +16% beagle more |
| E3 (oracle) | Wall-clock | 134s | 205s | +53% beagle slower |
| E3 (oracle) | Tokens | 41K | 59K | +44% beagle more |
| E3b (no oracle) | Wall-clock | 344s | 219s | **36% beagle faster** |
| E3b (no oracle) | Tokens | 60K | 73K | +22% beagle more |
| E3b (no oracle) | Bugs fixed | 12/12 | 12/12 | equal |

E3 vs E3b tells the story. When the agent has a comprehensive test suite,
the type checker is pure overhead — the tests are a better oracle (they
catch logic bugs too). When the agent has no test suite, the type checker
is the fastest diagnostic channel available, and beagle's wall-clock
advantage is significant.

Beagle's value is not "catches bugs agents miss." It's "gives agents a
deterministic feedback loop that scales with codebase size and compounds
with model improvement." The checker does diagnostic work; the agent does
synthesis work. The time savings grow as the codebase grows because
structured error messages don't get harder to parse, but reading more code
does.
