# E19: Agent macro authoring — results

**Date:** 2026-05-21

**Status:** Scoped and task files written. Conditions A and B verified
(compile + run with identical output). Ready for agent trial runs.

## Setup verification

Both v1 (path-only) and v2 (method+path) versions compile and produce
identical output across baseline and proc macro conditions.

### LOC comparison (single router, below crossover)

| Variant | v1 | v2 | Delta (lines) | Change sites |
|---------|:--:|:--:|:-------------:|:------------:|
| A: Baseline | 15 | 17 | +2 | 7 hunks |
| B: Proc macro | 26 | 29 | +3 | 8 hunks |

At one router, the macro version is longer (expected — below E18's
crossover of 2-4 instances). The spec change touches similar line counts
in both. **The compression advantage appears at 3+ routers.**

### What this means for E19's design

The interesting question isn't "does the macro compress" (E18 answered
that). It's:

1. **Can an agent write a proc macro at all?** (Condition B vs C)
2. **Does beagle-expand close the feedback loop?** (iteration count)
3. **Does the cheatsheet help?** (B vs C wall time + iterations)

## Remaining work

- [ ] Run Condition B: give agent cheatsheet.md + proc macro docs, measure
- [ ] Run Condition C: give agent cheatsheet.md only, measure
- [ ] Compare iterations and wall time
- [ ] Apply spec change to agent-produced code, measure propagation
- [ ] Record final metrics in this file
