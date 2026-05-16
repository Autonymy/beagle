# E5 Bug Detection — Experiment Results

**Date:** 2026-05-16
**Domain:** Event-sourced e-commerce pipeline (8 modules, ~3000 LOC per track)
**Task:** Find and fix 40 injected bugs using available tooling
**Model:** Claude Opus 4 (both tracks, same model)

## Experiments

### E5c: Positional constructors (beagle v0.2.0)

Beagle projections used `->RecordName` positional constructors (14+ args).

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Mean score | 63.7% | 68.3% |
| Std deviation | 1.5% | 5.5% |
| Bug surface | 178 lines | 164 lines |
| Checker errors | 0 (all runs) | n/a |

### E5d: `with` form (beagle v0.3.0)

Beagle projections use `(with state [:field value])` — typed record updates.
Same 40 bugs, adapted for new syntax.

| Trial | Beagle | Clojure |
|-------|--------|---------|
| Run 1 | 65% | 71% |
| Run 2 | 68% | 72% |
| Run 3 | 65% | 68% |

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Mean score | 66.0% | 70.3% |
| Std deviation | 1.7% | 2.1% |
| Bug surface | 163 lines | 164 lines |
| Checker errors | 0 (all runs) | n/a |

### E5c → E5d changes

| What changed | E5c | E5d |
|---|---|---|
| Beagle projections syntax | Positional `->Record` (14 args) | `(with state [:field val])` |
| Beagle bug surface | 178 lines | 163 lines (now equal to clojure) |
| Beagle mean score | 63.7% | 66.0% (+2.3pp) |
| Clojure mean score | 68.3% | 70.3% (+2.0pp) |
| Gap | -4.6pp | -4.3pp |
| Clojure std dev | 5.5% | 2.1% (both tracks consistent now) |
| Beagle projections score | 35% | 20% (worse — see analysis) |

## Per-Module Scores (E5d, averaged across 3 runs)

| Module | Beagle | Clojure | Notes |
|--------|--------|---------|-------|
| events | 71% | 100% | Clojure fixes constructors perfectly |
| projections | 20% | 68% | Beagle over-fixes; see below |
| commands | 75% | 70% | Beagle edge |
| handlers | 85% | 93% | Clojure edge |
| queries | 76% | 66% | Beagle edge |
| pipeline | 76% | 45% | Beagle strong edge |
| notifications | 52% | 50% | Similar |
| analytics | 100% | 100% | Both perfect |

## Analysis

### Headline

The `with` form closed the bug-surface gap (163 vs 164 lines, previously 178 vs 164)
and slightly improved beagle's score (+2.3pp) — but clojure still wins on raw
line-level accuracy (70.3% vs 66.0%). The gap narrowed from -4.6pp to -4.3pp.

### Why `with` didn't flip the result

1. **Over-fixing in projections.** Beagle agents see checker errors like
   `with ShipmentState: field :delivered-at expected Long?, got String` and fix
   by changing `"pending"` to `nil`. But the golden removes the `[:delivered-at ...]`
   update entirely. The agent's fix is correct (passes checker) but adds an extra
   line. Similarly, `apply-payment-failed-to-payment` — agents add nil guards to
   `subs` instead of removing the txn-id modification entirely.

2. **Agent doesn't know what to delete.** The type checker says "wrong type" but
   doesn't say "this entire update shouldn't exist." The agent makes the update
   well-typed rather than removing it. This is the fundamental gap between
   type-guided repair and intent-based repair.

3. **Missing match cases aren't detectable by types.** Both tracks miss the same
   3 dispatch cases (OrderCancelled, CustomerTierChanged, RefundIssued) in some
   runs. The type checker can't help because `[_ state]` is a valid catch-all.

4. **Clojure's projections.clj is stable across runs.** All 3 clojure runs produce
   identical projections diffs (11 remaining lines = 2 missing match cases). Beagle
   produces 20-22 remaining lines because of diverse over-fix patterns.

### What the experiment proves

1. **Verification is still the real advantage.** 0 checker errors on all 6 beagle
   runs. No type-level regression introduced by any fix. This guarantee has no
   clojure equivalent.

2. **Consistency improved.** Both tracks now have low std dev (beagle 1.7%, clojure
   2.1%). The bug-surface equalization helped — E5c's clojure variance (5.5%) was
   partly due to the surface asymmetry.

3. **The gap is small and structural.** 4.3pp is within the range where scoring
   methodology matters. The remaining gap comes from a specific failure mode:
   agents that make type-valid fixes that don't match golden intent.

4. **Line-level diff punishes "correct but different" fixes.** Both
   `(with state [:delivered-at nil])` and removing the update entirely produce
   the same runtime behavior — but only one matches golden. A behavioral test
   suite would likely close or reverse the gap.

### What `with` did help

- Eliminated the bug-surface asymmetry (178 → 163, now ~equal to clojure)
- The checker now catches field-type mismatches in updates (`:delivered-at "pending"`)
- Projections are 50% shorter — less code for agents to reason about
- Beagle's overall score improved (+2.3pp from E5c)

### What `with` didn't help

- Over-fixing (correct-but-different repairs) is unchanged
- Missing match cases still undetectable by types
- Events.rkt (cross-record accessor bugs) still scores 71% vs 100%

## Infrastructure

- `buggy-clean/` — buggy files with no bug-location hints
- `buggy-original/` — original buggy files (for reference only)
- `trials/` — per-run working directories (preserved for reproducibility)
- `prompts/` — exact agent prompts used
- `bin/score-trial` — automated scoring against golden
- `bin/run-trial` — trial directory setup

## Conclusion

Beagle's value is **verification** (0 proven type errors) and **consistency**
(predictable output), not raw accuracy. On both E5c and E5d, Claude Opus 4 can
read ~3000 LOC and find most bugs by inspection — the type checker adds proof
but not detection power at this model capability level.

The remaining accuracy gap (4.3pp) is entirely in projections (beagle 20% vs
clojure 68%) and events (71% vs 100%). Both are attributable to "correct but
different" fixes that a behavioral test suite would resolve.

## E5e: Behavioral Scoring

**Date:** 2026-05-16
**Hypothesis:** Line-level diff scoring penalizes "correct but different" fixes.
A behavioral test suite that checks runtime outcomes should close the gap.

### Method

40 behavioral tests (one per injected bug) that exercise the specific behavior
each bug breaks. Tests check observable outcomes (return values, state after
event application) not implementation details. "Correct but different" fixes
pass if they produce correct behavior.

### Clojure Results

| Trial | Line-diff (E5d) | Behavioral (E5e) |
|-------|:---:|:---:|
| Run 1 | 71% | 90% |
| Run 2 | 72% | 90% |
| Run 3 | 68% | 90% |

| Metric | Line-diff | Behavioral |
|--------|:---:|:---:|
| Mean score | 70.3% | 90.0% |
| Std deviation | 2.1% | 0.0% |

### Consistently Failing Bugs (all 3 runs)

| Bug | Category | Module | Description |
|-----|----------|--------|-------------|
| BUG-09 | F: missing case | projections | CustomerTierChanged not handled |
| BUG-10 | F: missing case | projections | RefundIssued not handled in payment |
| BUG-11 | B: nil access | commands | paid-amount nil causes NPE |
| BUG-18 | A: wrong field | handlers | cancelled-at used instead of reason |

All 4 are bugs the agent consistently fails to detect by code inspection.
BUG-09 and BUG-10 are missing match cases (category F) — invisible to
type checkers on both tracks. BUG-11 is a nil-handling issue that only
manifests at runtime. BUG-18 is a field-name confusion that produces a
Long where String is expected.

### Beagle Results

| Trial | Line-diff (E5d) | Behavioral (E5e) |
|-------|:---:|:---:|
| Run 1 | 65% | 86.8% |
| Run 2 | 68% | 84.2% |
| Run 3 | 65% | 84.2% |

| Metric | Line-diff | Behavioral |
|--------|:---:|:---:|
| Mean score | 66.0% | 85.1% |
| Std deviation | 1.7% | 1.5% |

Note: 2 of 40 tests excluded (BUG-38, BUG-39) — functions added to golden
after trials ran. Denominator is 38 for beagle.

### Head-to-Head Behavioral Comparison

| Metric | Beagle | Clojure |
|--------|:---:|:---:|
| Mean behavioral score | 85.1% | 90.0% |
| Std deviation | 1.5% | 0.0% |
| Gap | -4.9pp | — |
| Checker errors (all runs) | 0 | n/a |

### Beagle-Only Failures (not shared with clojure)

| Bug | Runs | Description |
|-----|------|-------------|
| BUG-27 | 1,2,3 | NPE: nil delivered-at in arithmetic (agent fix too strict) |
| BUG-29 | 1,2,3 | total-revenue sums "delivered" only, not "non-cancelled" |
| BUG-09 | 2 | CustomerTierChanged match case not added |
| BUG-24 | 3 | OrderDelivered dispatch not handled in handler |

### Shared Failures (both tracks)

| Bug | Category | Description |
|-----|----------|-------------|
| BUG-10 | F: missing case | RefundIssued not handled in payment |
| BUG-11 | B: nil access | paid-amount nil causes NPE |
| BUG-18 | A: wrong field | cancelled-at used instead of reason |

BUG-09 (CustomerTierChanged missing case) fails on all 3 clojure runs but
only 1 beagle run — beagle's exhaustive match note may have helped in 2/3.

### Analysis

The +19.1pp improvement (66.0% → 85.1%) for beagle confirms that line-diff
scoring was penalizing "correct but different" fixes. The clojure improvement
is larger (+19.7pp, 70.3% → 90.0%) because clojure's fixes are more consistent.

The remaining gap (4.9pp) comes from two categories:
1. **Over-strict fixes** (BUG-27, BUG-29): The agent makes type-valid fixes that
   are semantically too restrictive. The type checker says "this is fine" but the
   runtime behavior doesn't match the test expectation.
2. **Inconsistent detection** (BUG-09, BUG-24): Beagle sometimes catches
   missing dispatch cases (2/3 for BUG-09) where clojure never does.

Key finding: **beagle's verification advantage (0 checker errors) is confirmed,
but its behavioral accuracy gap is real.** The gap is structural: agents guided
by type errors tend toward over-restrictive fixes.

### Infrastructure

Behavioral scoring uses per-track adapters to bridge naming differences:
- `verify/adapter-beagle.clj`: maps `make-order-state`, `handle-event`,
  `shipment-by-carrier` + stubs for post-trial functions
- `verify/adapter-clojure.clj`: no-op (canonical API matches)
- `bin/score-behavioral`: passes adapter via `-J-De5.adapter=` system property

Emitter fixes applied (unblocked beagle behavioral scoring):
- Cross-module match arms emit `let` bindings for destructured fields
- `instance?` uses fully-qualified class names (`pipeline.events.OrderPlaced`)
- Beagle module requires emit `:refer :all :as alias`

## Next steps

1. ~~Semantic scoring via behavioral tests~~ → **Done (E5e)**
2. ~~Fix beagle emitter: match destructuring + qualified accessor emission~~ → **Done**
3. ~~Re-score beagle trials with behavioral tests~~ → **Done**
4. ~~Exhaustive match warnings to flag missing dispatch cases~~ → **Done (sibling heuristic)**
5. Smaller model experiments (beagle's advantage should grow as model degrades)
6. Intent-preserving repair: teach the checker to suggest "remove this update"
   not just "fix the type"
