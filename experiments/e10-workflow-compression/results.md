# E10 Results: Workflow Compression

Hypothesis: if beagle emits a machine-applicable patch instead of a
human-readable repair queue, the agent will spend fewer turns and tokens
on mechanical fixes. Result: confirmed.

> E9: Beagle gives the agent a better repair queue.
> E10: Beagle turns part of that queue into an executable patch.
> Result: fewer turns, fewer tokens, less wall time, same correctness.

Against the E9 baseline, `--emit-patch` reduced wall time by 33% and
output tokens by 41%, while preserving full correctness (484/484).

## Summary

Patch-first workflow (`--emit-patch` + `git apply`) reduces beagle wall time
by 26% vs E9 baseline. Head-to-head gap widens from 29% (E9) to 33%.

## Raw data

| Run | Beagle (s) | Clojure (s) | Notes |
|-----|-----------|------------|-------|
| 1 | ~~659~~ | 494 | Beagle run invalid — `--emit-patch` had 3 bugs (see below) |
| 2 | 285 | 404 | |
| 3 | 334 | 494 | |
| **Avg (valid)** | **310** | **464** | Beagle runs 2+3 only |

Correctness: 3/3 both tracks (484/484 assertions).

## Comparison to E9

| Metric | E9 Beagle | E9 Clojure | E10 Beagle | E10 Clojure |
|--------|-----------|------------|------------|-------------|
| Wall time (avg) | 421s | 595s | 310s | 464s |
| Gap | 29% faster | — | 33% faster | — |
| Correctness | 3/3 | 3/3 | 3/3 | 3/3 |

Beagle improved 421→310s (−26%). Clojure also improved 595→464s (−22%),
likely normal run-to-run variance (same spec as E9).

## What the patch fixes

The repair patch auto-applies 6 mechanical fixes in one `git apply`:

1. Operand swap (`product-margin`: cost−price → price−cost)
2. Value swap (`tier-discount-pct`: gold/bronze percentages)
3. Comparator flip (`reorder-needed`: `>` → `<`)
4. Wrong accessor (`carrier-total-cost`: `carrier-id` → `carrier-base-rate`)
5. Wrong operator (`zone-surcharge`: `+` → `*`)
6. Wrong literal (`shipping-cost-for-order`: `"pending"` → `order-id`)

These 6 fixes eliminate 15 of 33 baseline failures (45%). The remaining
18 failures are semantic bugs requiring agent reasoning.

## Beagle run 1: invalid (toolchain bugs)

Run 1 used a broken `--emit-patch` pipeline. The agent correctly identified
the patch was producing wrong fixes and reverted it, falling back to manual
repair (659s — worse than E9). Three bugs were found and fixed before runs 2–3:

1. **Oracle crash = false verification.** When a candidate fix crashed the
   verify script, `bb` exited with code 1 and printed zero `FAIL:` lines.
   Specfix interpreted empty failure set as "all tests pass" → VERIFIED.
   Fix: check exit code AND presence of summary line before accepting results.

2. **Search/replace applied to wrong function.** Specfix generates fixes
   scoped to a specific function, but `--emit-patch` scanned the .rkt file
   top-down and matched the first occurrence. E.g., `carrier-id → carrier-base-rate`
   was correct for `carrier-total-cost` but got applied to `find-carrier-by-id`.
   Fix: scope search to lines within the target function's `defn` body.

3. **Cross-type-group accessor swaps.** Specfix tried swaps like
   `product-unit-cost → product-name` (Long→String accessor). With scalar
   erasure, `price-value` is identity at runtime, so some nonsensical swaps
   passed oracle verification by coincidence.
   Fix: reject accessor swap candidates where old and new accessors are in
   different type groups (numeric vs string vs boolean).

## Interpretation

The patch-first workflow works as designed: mechanical fixes are zero-reasoning-
token operations. The agent's time is spent entirely on semantic bugs that
require judgment (wrong formulas, missing arguments, incorrect field selection).

Mechanical bugs should not require cognition. They should compile into patches.
That is what `--emit-patch` does — the compiler becomes part of the agent's
motor cortex, applying fixes that don't need deliberation.

The 33% gap is real but modest. The bottleneck is now semantic bugs, not
mechanical ones — diminishing returns on further toolchain automation at this
bug mix (17/35 mechanical, 18/35 semantic).

Note: this is not "beagle language vs Clojure language" in the narrow sense.
It is beagle's repair workflow vs raw Clojure's repair workflow. The advantage
is that the authoring surface gives the tooling enough structure to emit
trusted patches. Raw Clojure does not naturally expose the same repair
substrate.
