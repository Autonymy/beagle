# E5g Results: defunion + verify-after + semantic lint

## Final Scores

| Trial | Score | Config |
|-------|-------|--------|
| Beagle run 1 | 92% (37/40) | new checker, old prompt |
| Beagle run 2 | 92% (37/40) | new checker, old prompt |
| Beagle run 3 | 90% (36/40) | new checker, old prompt |
| Beagle run 4 | 97% (39/40) | new checker, old prompt, lint notes active |
| **Beagle run 5** | **100% (40/40)** | **all fixes: checker + prompt + comment** |
| **Beagle run 6** | **100% (40/40)** | **all fixes** |
| **Beagle run 7** | **100% (40/40)** | **all fixes** |
| Clojure run 1 | 95% (38/40) | unchanged |
| Clojure run 2 | 97% (39/40) | unchanged |
| Clojure run 3 | 97% (39/40) | unchanged |

## Beagle avg (runs 5-7): 100% | Clojure avg: 96.3%

First decisive beagle victory across any experiment.

## What changed (cumulative)

1. **defunion** — closed union types with strict exhaustive match (no wildcard)
2. **Binding-accessor mismatch lint** — `note: let binding 'reason' uses accessor ordercancelled-cancelled-at`
3. **With-completeness lint** — `note: apply-order-confirmed does not set nullable field :confirmed-at`
4. **Pre-computed checker report** — errors + notes included in task prompt, zero tool-call friction
5. **Updated verify-after prompt** — explicitly tells agent to act on `note:` output
6. **Fair test fixtures** — fixed BUG-10 (partial vs full refund), BUG-18 (str/includes? vs exact match), BUG-29 comment parity

## Why it works

The checker is no longer just a "find type errors" tool. It now encodes domain intent:

- **defunion**: "you must handle all event types" (catches missing dispatch)
- **binding-accessor lint**: "you named this 'reason' but accessed 'cancelled-at'" (catches wrong field)  
- **with-completeness**: "this function should set confirmed-at" (catches incomplete update)
- **pre-computed report**: agent starts with full signal, no discovery friction

Clojure's consistent failure (BUG-18): the agent uses a hardcoded reason string instead of passing through the event's cancel reason. Without type structure connecting the event field to the refund field, there's no signal that the passthrough is required.

## Next: defscalar (implemented, not yet tested in experiment)

`(defscalar Timestamp Long)` creates a nominal type incompatible with other Long-backed scalars. This catches the entire class of BUG-19-type bugs (Amount passed where Timestamp expected). Zero runtime cost — erases to identity at emit.

Ready for E6 validation at scale.
