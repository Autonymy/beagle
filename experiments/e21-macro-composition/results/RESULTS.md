# E21: Macro composition — results

**Date:** 2026-05-21

## Finding

Proc macros compose. A single `defentity-api` macro generates the full
vertical slice (defrecord + getters + CRUD endpoints) from one field
spec. Field changes propagate automatically through all generated code.

## Conditions tested

| Condition | Approach | LOC | Field rename sites |
|-----------|----------|:---:|:------------------:|
| A: Shared spec | Separate `defentity` + `defapi`, same field list | 40 | 3 (entity + api + test) |
| B: Combined | Single `defentity-api` generates both | 39 | 2 (spec + test) |

Both produce identical output for the same entities.

## What works

- **`(Vec Form)` splicing composes.** A single macro returning 6+ forms
  (defrecord + 3 getters + 3 API endpoints) splices correctly into the
  module.
- **`append` combines form lists.** The entity forms and API forms are
  built separately then `append`ed into one `(Vec Form)` return.
- **Name computation is straightforward.** `string->symbol` + `format`
  generates both entity accessors (`User-name`) and API functions
  (`list-users`, `get-user`, `create-user`) from the same name.

## What doesn't work (Condition C: chained)

Condition C (defapi calling defentity internally) was not attempted.
Proc macros can't call other proc macros at expansion time — the
registry is populated during pass 1, and expansion happens during
pass 2, but one macro's body can't invoke `expand-fully` on another
macro's invocation. This would require making the registry available
inside the proc macro namespace, which is possible but not yet built.

## Practical implication

For macro composition today, use Condition B: combine the generation
logic into a single macro that returns all forms. This works because
the field spec is just data (lists of `(name : Type)` tuples) that
any Racket code can destructure.

The crossover for `defentity-api` vs hand-written: **2 entities.**
At 3 entities, the macro saves ~30 lines. At 10 entities, ~120 lines.
Field renames touch 1 site per entity (the macro call) instead of
6+ sites (record + getters + 3 API functions).
