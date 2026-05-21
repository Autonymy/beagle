# E21: Macro composition

## Question

Can proc macros consume the output of other proc macros? Specifically:
can `defapi` read the field structure produced by `defentity` to
generate typed CRUD endpoints?

## Why this matters

Single macros compress individual patterns (E18). Composed macros
compress entire layers — the entity definition cascades into API
scaffolding, validation, serialization. If composition works, a
single `defentity` invocation could generate the full vertical slice.

## Setup

Two macros, one consuming the other's output:

1. **`defentity`** (from E18): generates defrecord + typed getters
2. **`defapi`** (new): consumes the entity's field list to generate
   typed CRUD endpoints (list, get-by-id, create with typed params)

The composition question: does `defapi` need to re-parse the entity
spec, or can it read from the already-expanded defrecord?

## Conditions

| Condition | How macros interact |
|-----------|-------------------|
| **A: Shared spec** | Both macros receive the same field list directly |
| **B: Sequential** | `defapi` receives entity name + field list, generates its own forms |
| **C: Chained** | `defapi` calls `defentity` internally, returning both sets of forms |

## Task

Define 3 entities (User, Product, Order) with fields. Generate:
- Record + getters (from defentity)
- list-Xs, get-X, create-X endpoints (from defapi)

## Metrics

| Metric | How measured |
|--------|-------------|
| LOC | Content lines for 3 entities |
| Composition overhead | Extra lines for the composition vs separate macros |
| Correctness | Compiled output runs in Babashka |
| Field propagation | Change a field name → does it propagate through both macros? |

## Expected finding

Condition A (shared spec) works today — both macros take the field
list as input. Condition C (chained) requires proc macros calling
other proc macros, which may need explicit support in the macro
registry.

## Files

```
experiments/e21-macro-composition/
  DESIGN.md              — this file
  tasks/
    A-shared-spec.bgl    — both macros receive field list
    B-sequential.bgl     — separate invocations, shared field spec
    C-chained.bgl        — defapi calls defentity internally
  results/
    RESULTS.md           — measurements + analysis
```
