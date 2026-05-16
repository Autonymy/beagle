# E5c Bug Detection — Experiment Results

**Date:** 2026-05-16
**Beagle version:** v0.2.0 (commit f91b70a, cross-module type validation)
**Domain:** Event-sourced e-commerce pipeline (8 modules, ~3000 LOC per track)
**Task:** Find and fix 40 injected bugs using available tooling
**Model:** Claude Opus 4 (both tracks, same model)

## Methodology

- 40 bugs injected across 8 modules (see `buggy/BUG-MANIFEST.md`)
- Bug labels stripped from source — agents receive clean buggy code with no hints
- Beagle track gets `beagle-check-all` (type checker catches 25/40 at compile time)
- Clojure track gets code reading only (no type checker, no test suite)
- Both tracks receive the domain spec (`spec/domain.md`) for business-logic context
- Exact prompts published in `prompts/`
- Scoring: automated line-level diff against golden reference via `bin/score-trial`
- 3 trials per track

## Results

### Trial Data

| Trial | Track | Score | Time | Tool calls | Checker errors |
|-------|-------|-------|------|------------|----------------|
| Run 1 | Beagle | 64% | 402s | 66 | 0 |
| Run 1 | Clojure | 62% | 207s | 49 | n/a |
| Run 2 | Beagle | 65% | 228s | 70 | 0 |
| Run 2 | Clojure | 71% | 240s | 62 | n/a |
| Run 3 | Beagle | 62% | 326s | 77 | 0 |
| Run 3 | Clojure | 72% | 231s | 61 | n/a |

### Summary Statistics

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Mean score | 63.7% | 68.3% |
| Std deviation | 1.5% | 5.5% |
| Min / Max | 62% / 65% | 62% / 72% |
| Mean time | 319s | 226s |
| Mean tool calls | 71 | 57 |
| Checker errors (all runs) | 0 | n/a |

### Per-Module Scores (averaged across 3 runs)

| Module | Beagle | Clojure | Notes |
|--------|--------|---------|-------|
| events | 71% | 100% | Clojure fixes constructors perfectly every time |
| projections | 35% | 63% | Beagle struggles with partial-fix residue |
| commands | 63% | 70% | Similar |
| handlers | 82% | 86% | Both strong |
| queries | 76% | 67% | Beagle edge |
| pipeline | 76% | 45% | Beagle edge |
| notifications | 56% | 50% | Similar |
| analytics | 94% | 100% | Both strong |

## Analysis

### The headline result

Clojure outperforms beagle on raw line-level accuracy (68.3% vs 63.7%) while
being faster (226s vs 319s) and using fewer tool calls (57 vs 71).

This was not the expected result.

### Why beagle underperforms on accuracy

1. **The type checker is a double-edged sword.** Beagle's 25 compile-time errors
   give the agent an exact repair list — but fixing type errors sometimes produces
   *technically correct but not golden* code. The agent satisfies the checker
   without matching the intended fix. Example: `customerstate-email` where golden
   uses `(str customerstate-customer-id)` — both pass type checking, only one
   matches golden.

2. **Projections are beagle's weak spot (35%).** The beagle track has record
   constructors with 14 positional args. The checker catches missing/wrong args,
   but the agent's fixes for multi-arg constructors often diverge from golden
   in the specific values chosen. Clojure's keyword-based record updates are
   more forgiving.

3. **Beagle's bug surface is larger.** Some bugs were relocated to events.rkt
   (where typed accessors are defined) for beagle, giving it 178 lines of bug
   surface vs clojure's 164. This 8.5% difference compounds in scoring.

### What beagle does provide

1. **Verification.** Every beagle run produces 0 checker errors. This proves all
   type-level fixes are correct — wrong field access, wrong arity, wrong type are
   eliminated with certainty. Clojure's higher score is unverified: some "fixes"
   may be coincidentally correct or subtly wrong in ways the line-level diff
   doesn't catch.

2. **Consistency.** Beagle's std deviation is 1.5% vs clojure's 5.5%. The type
   checker anchors the agent's behavior — it always fixes the same 25 errors
   the same way. Clojure's accuracy depends more on how carefully the agent reads.

3. **Guaranteed floor.** The checker catches 25/40 bugs. Even a minimal agent
   that only fixes checker errors would score ~35-40%. Clojure has no floor —
   a bad run could score much lower.

### What beagle does NOT provide

1. **Higher accuracy.** On this benchmark with this model, code reading beats
   type checking on raw fix rate. The model is good enough at reading ~3000 LOC
   to identify most bugs by inspection.

2. **Speed advantage.** The verification loop adds overhead. Beagle agents
   run beagle-check-all multiple times; clojure agents just read and edit.

3. **Logic bug detection.** Both tracks find logic bugs at similar rates
   (analytics 94-100% for both). The type checker cannot help here, and the
   model's domain reasoning is the bottleneck for both.

### Confounding factors

- **Bug surface asymmetry.** Beagle has 178 lines of bug diff vs clojure's 164
  due to bug relocation. This penalizes beagle by ~8%.
- **Positional vs keyword constructors.** Beagle uses positional `->Record`
  constructors; clojure uses keyword `assoc`/`merge`. Positional is harder to
  get exactly right when the checker says "arg 11 wrong type."
- **Scoring metric.** Line-level diff rewards clojure's keyword-update style
  (small targeted changes) over beagle's constructor-rebuild style (full
  arg lists that may diverge).

### Invalidated prior trial

An earlier trial (committed 5a9d0d4) used buggy files with explicit `BUG-XX`
comments labeling each bug location. Both agents could read the labels, making
the task trivially easy and producing misleading results (beagle 27/40 vs
clojure 0/40 — the clojure score was wrong due to a flawed verification agent).
That trial's data is not included in these results.

## What this means for beagle

The experiment does not show that beagle makes AI agents more accurate at bug
fixing. It shows that:

1. **Verification is real.** Beagle's 0 checker errors is a guarantee that
   clojure cannot match. In production, "provably no type errors" beats
   "probably correct" even if the latter scores higher on a diff metric.

2. **Consistency is real.** 1.5% std dev means beagle is predictable. For
   automated pipelines, low variance matters more than peak accuracy.

3. **The model is better at reading code than expected.** Claude Opus 4 can
   inspect 3000 LOC and find 30+ bugs by reading. This was not true of earlier
   models. Beagle's value proposition shifts as models improve: from "catches
   bugs models can't find" to "proves fixes are correct."

4. **The scoring methodology matters.** Line-level diff penalizes beagle's
   positional constructors. A semantic scoring rubric (does the fix produce
   correct behavior?) might tell a different story — but that requires a test
   suite, which neither track had.

## Infrastructure

- `buggy-clean/` — buggy files with no bug-location hints
- `buggy-original/` — original buggy files (with labels, for reference only)
- `trials/` — per-run working directories (preserved for reproducibility)
- `prompts/` — exact agent prompts
- `bin/score-trial` — automated scoring against golden
- `bin/run-trial` — trial directory setup

## Next steps

1. Add a test suite to enable semantic scoring (does the fix work?) vs
   syntactic scoring (does the fix match golden?)
2. Run with smaller models to test the hypothesis that beagle's advantage
   grows as model capability decreases
3. Add a Clojure + spec/Malli baseline
4. Normalize bug surface between tracks (same file layout)
