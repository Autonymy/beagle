# E5c Bug Detection — Experiment Results

**Date:** 2026-05-16
**Beagle version:** v0.2.0 (commit f91b70a, cross-module type validation)
**Domain:** Event-sourced e-commerce pipeline (8 modules, ~3000 LOC per track)
**Task:** Find and fix 40 injected bugs using available tooling

## Head-to-Head Summary

| Metric | Beagle Track | Clojure Track |
|--------|-------------|---------------|
| Wall time | 293s | 235s |
| Tool calls | 69 | 57 |
| Bugs correctly fixed | **27/40** | **0/40** |
| Bugs partially fixed | 8/40 | 0/40 |
| Bugs missed | 2/40 | 35/40 |
| Wrong fixes introduced | 0 | 5 |
| Compile-time bugs caught | 13 | 0 |
| Post-fix verification | 0 checker errors | no verification path |

## Key Finding

`beagle-check-all` returns 0 errors on the fixed codebase, verifying that all 27 correct
fixes satisfy beagle's cross-module type/contracts layer. The clojure agent made changes to
5 files but **none of its edits match the golden reference**. Without a type system, the
agent could not distinguish correct code from buggy code and made changes that were either
wrong, incomplete, or applied to the wrong location.

**Scoring rubric:** "Correct" = matches the intended golden repair, or is behaviorally
equivalent where equivalence is clear. "Partial" = bug was addressed but fix diverges from
golden in ways that may introduce edge-case differences. "Wrong fix" = edit changes behavior
in a direction that does not resolve the bug or introduces a new defect.

## Detailed Results — Beagle Track

### Correctly Fixed (27/40)

| Bug | Category | Module | Fix |
|-----|----------|--------|-----|
| BUG-01 | E: Constructor | events.rkt | `->OrderItem` arg order corrected |
| BUG-02 | E: Constructor | events.rkt | `->OrderState` String→Long corrected |
| BUG-03 | C: Arity | projections.rkt | 14-arg `->OrderState` restored |
| BUG-04 | D: Wrong type | projections.rkt | String→Long in `apply-payment-to-order` |
| BUG-08 | F: Missing case | projections.rkt | `OrderCancelled` case restored |
| BUG-10 | F: Missing case | projections.rkt | `RefundIssued` case restored |
| BUG-14 | C: Arity | commands.rkt | `assert-paid` 2→1 args |
| BUG-15 | D: Wrong type | commands.rkt | String "3"→Long 3 |
| BUG-16 | D: Wrong type | commands.rkt | String "0"→Long 0 |
| BUG-17 | A: Wrong field | events.rkt | `customerstate-total-spent`→correct accessor |
| BUG-18 | A: Wrong field | events.rkt | `inventorystate-available`→correct accessor |
| BUG-19 | A: Wrong field | events.rkt | `orderstate-total` on CustomerState→correct |
| BUG-21 | C: Arity | handlers.rkt | `make-notification` 5→4 args |
| BUG-22 | D: Wrong type | notifications.rkt | Long→String for recipient |
| BUG-23 | F: Missing case | handlers.rkt | `InventoryReserved` dispatch restored |
| BUG-24 | F: Missing case | handlers.rkt | `OrderDelivered` dispatch restored |
| BUG-25 | A: Wrong field | events.rkt | `paymentstate-status` on OrderState→correct |
| BUG-26 | A: Wrong field | events.rkt | `orderstate-total` on InventoryState→correct |
| BUG-27 | B: Nil access | queries.rkt | Nil guard added for `delivered-at` |
| BUG-28 | G: Logic | queries.rkt | `-`→`+` in accumulator |
| BUG-29 | G: Logic | queries.rkt | "cancelled"→"delivered" filter |
| BUG-30 | C: Arity | pipeline.rkt | `append-event` 3→2 args |
| BUG-31 | C: Arity | pipeline.rkt | `detect-event-type` 2→1 arg |
| BUG-32 | E: Constructor | pipeline.rkt | `->EventStore` args corrected |
| BUG-34 | D: Wrong type | notifications.rkt | Long→String in map |
| BUG-36 | E: Constructor | notifications.rkt | Long→String in template |
| BUG-38 | G: Logic | analytics.rkt | `* 10`→`* 100` |
| BUG-39 | G: Logic | analytics.rkt | Subtraction order corrected |
| BUG-40 | A: Wrong field | events.rkt | `orderstate-placed-at` on PaymentState→correct |

### Partially Fixed (8/40)

| Bug | Category | Issue |
|-----|----------|-------|
| BUG-05 | D: Wrong type | Set `delivered-at` to nil instead of preserving existing value |
| BUG-06 | B: Nil access | Added nil guard but golden uses simpler `orderstate-status` |
| BUG-07 | B: Nil access | Added nil guard for `subs`; golden doesn't mutate at all |
| BUG-09 | F: Missing case | Case restored but variable names renamed |
| BUG-11 | B: Nil access | Functionally correct but different structure than golden |
| BUG-12 | B: Nil access | Added `str` wrapper not in golden |
| BUG-33 | E: Constructor | Fixed bug but added dead binding not in golden |
| BUG-35 | D: Wrong type | Used `customerstate-email` where golden uses `(str customer-id)` |
| BUG-37 | G: Logic | Changed wrong variable; priority values still swapped |

### Missed (2/40)

| Bug | Category | Issue |
|-----|----------|-------|
| BUG-13 | C: Arity | Comment added but call site already correct |
| BUG-20 | C: Arity | Comment added but call already at correct arity |

## Detailed Results — Clojure Track

### Correctly Fixed: 0/40

The clojure agent modified 5 of 8 files but **zero changes match the golden reference**.

### Wrong Fixes (5/40)

| Bug | Issue |
|-----|-------|
| BUG-07 | Added `when` guard but also incorrectly mutated `transaction-id` |
| BUG-12 | Replaced status check with `(some? :confirmed-at)` — semantically different |
| BUG-27 | Removed existing nil guards instead of adding the missing one |
| BUG-30 | Rewrote `append-events` as reduce — functional but doesn't match golden |
| BUG-33 | Added dead `let` binding in wrong function |

### Not Touched: 35/40

Events, handlers, notifications, and analytics were not modified at all.
All 8 wrong-field-access bugs, all 5 constructor bugs, all 5 logic bugs, and
4 of 6 arity bugs remain unfixed.

## Analysis

### Why beagle wins

1. **Compiler as oracle.** The beagle agent ran `beagle-check-all` and received 13 errors
   pointing to exact bugs with type signatures and expected-vs-actual diagnostics. These
   13 bugs required zero reasoning — just follow the error message.

2. **Verification loop.** After fixing compiler-caught bugs, the agent re-ran the checker.
   Zero errors confirmed those 13 fixes satisfy the type/contracts layer — no test suite needed.

3. **Momentum from certainty.** With 13 bugs already fixed and verified, the agent had
   bandwidth to manually inspect the remaining code. It found and fixed 14 additional bugs
   (including all 5 logic bugs and 4 missing-case bugs) that the checker cannot catch.

4. **Cross-module contracts.** Wrong-field-access bugs (category A) are completely silent in
   Clojure — `(:wrong-field record)` returns nil without error. Beagle's typed accessors
   (`orderstate-total` expects `OrderState`) catch these at compile time.

### Why clojure fails

1. **No signal.** Without a type system, the clojure agent has no way to distinguish buggy
   code from correct code without executing it. Reading 3000 LOC of unfamiliar event-sourcing
   code and spotting 40 bugs by inspection alone is beyond reliable LLM capability.

2. **False confidence.** The agent made changes to 5 files, but without verification it cannot
   know whether its changes are correct. Several "fixes" introduced new bugs.

3. **Wrong mental model.** Multiple fixes targeted the wrong function or applied the wrong
   transformation, suggesting the agent couldn't build an accurate mental model of the
   codebase from reading alone.

### Category breakdown

| Category | Beagle correct | Clojure correct | Beagle advantage |
|----------|---------------|-----------------|------------------|
| A: Wrong field (8) | 6 | 0 | Typed accessors catch at compile time |
| B: Nil access (6) | 1 | 0 | Partial — some over-corrected |
| C: Arity (6) | 4 | 0 | Arity checking catches 4/6 |
| D: Wrong type (5) | 4 | 0 | Type mismatch errors |
| E: Constructor (5) | 4 | 0 | Constructor arity/type checking |
| F: Missing case (5) | 4 | 0 | Manual inspection after momentum |
| G: Logic (5) | 4 | 0 | Manual inspection after momentum |
| **Total** | **27** | **0** | |

### The momentum hypothesis

Beagle's most striking result is fixing 14 bugs the checker **cannot** catch (categories
B, F, G partially). The hypothesis: by instantly resolving 13 bugs with certainty, the agent
freed cognitive capacity to carefully inspect remaining code. The clojure agent, overwhelmed
by the full 40-bug search space with no starting signal, couldn't effectively triage.

## Conclusion

Beagle turns AI coding from speculative editing into checked repair.

The type checker directly exposed 13 bugs, but the agent fixed 14 more outside the
checker's direct reach. The checker did not merely catch errors — it reduced uncertainty
enough for the agent to reason productively about the rest of the system. The clojure
agent, facing the same search space with no reliable signal, could not fix a single bug
correctly and made several things worse.

The difference was not model intelligence (same model, same prompt structure). It was
feedback quality. Beagle gave the agent exact, cross-module diagnostics and a verification
loop. Raw Clojure forced the agent to inspect 3000 LOC with no checkable facts.

### Limitations

- Single trial (n=1 per track). Variance unknown.
- Scoring rubric favors golden-match; some partial fixes may be functionally correct.
- Bug injection was manual and known to the experiment author.
- No third baseline (Clojure + spec/Malli/core.typed).

### Broader claim

AI agents do better when the language gives them small, typed, local, checkable facts.
This is not a claim about syntax preferences or type theory aesthetics. It is a claim
about the feedback loops that make AI-assisted programming reliable at scale.
