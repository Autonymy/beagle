# E20: Macro-generated code in CNF — results

**Date:** 2026-05-21

## Finding

Query tools were blind to macro-generated code. Now fixed — they see
through expansions.

## Before fix

Query tools (`beagle-sig`, `beagle-fields`, `beagle-callers`,
`beagle-provides`) used `read-beagle-datums` to read raw source.
This returned the unexpanded `(defentity User ...)` invocation, not
the generated `(defrecord User ...)` and `(defn User-name ...)` forms.
The extractors pattern-matched on `defn`/`defrecord`/`def` and found
nothing.

`beagle-expand` already worked — it has its own macro registration
and expansion pass.

## Fix

Added `expand-datums` to `expand-tool.rkt` — returns expanded datum
list instead of printing it. Query.rkt now calls `read-expanded-datums`
(with fallback to `read-beagle-datums` on failure) instead of reading
raw datums.

## Test matrix results (after fix)

| Query | Target | Result | Correct? |
|-------|--------|--------|:--------:|
| `beagle-sig User-name FILE` | Generated getter | `User-name : [User -> String]` | yes |
| `beagle-fields User FILE` | Generated record | `name : String, email : String, age : Int` | yes |
| `beagle-callers User-name FILE` | Call sites | Empty (no callers in this file) | yes |
| `beagle-provides FILE` | Module exports | 3 records, 9 functions, all typed | yes |
| `beagle-expand FILE` | Expansion | All 3 records + 9 getters | yes |
| `beagle-check FILE` | Type check | Clean (no errors) | yes |

All 6 queries resolve generated names. The expansion boundary is
transparent to the query tools.

## Implication

Proc macros are not black boxes. Agents can:
- Query signatures of generated functions (`beagle-sig`)
- Inspect record fields from generated defrecords (`beagle-fields`)
- See all module exports including generated names (`beagle-provides`)
- Audit what macros produce (`beagle-expand`)

This means macro-generated APIs participate fully in the agent workflow.
Downstream agents can discover and type-check against generated code
the same way they would against hand-written code.

## What's not tested yet

- Cross-module import of macro-generated names (consumer.bgl importing
  from author.bgl) — requires `require` to resolve the expanded forms
- `beagle-callers` across files (consumer calling generated getters)
- LSP hover/go-to-definition on macro-generated symbols
