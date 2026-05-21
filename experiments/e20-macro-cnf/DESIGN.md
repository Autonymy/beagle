# E20: Macro-generated code in CNF

## Question

Does CNF (Canonical Normal Form) see through macro expansions? When
agent A defines a macro and agent B consumes the generated code, can
agent B's queries (sig, fields, callers, provides) resolve through the
expansion boundary?

## Why this matters

Proc macros generate top-level forms (defn, defrecord). If the query
tools only see the macro invocation and not the expanded forms, then
macro-generated APIs are invisible to downstream agents. This would
make macros a black box — usable but not queryable.

## Setup

Three-agent scenario:

1. **Agent A (author):** writes `defentity` proc macro (from E18)
2. **Agent B (consumer):** imports the module, calls generated functions
3. **Agent C (auditor):** uses query tools to inspect the codebase

## Test matrix

| Query | Target | Expected result |
|-------|--------|-----------------|
| `beagle-sig User-name FILE` | Generated getter | `(User-name (r : User)) : String` |
| `beagle-fields User FILE` | Generated record | `name : String, email : String, age : Int` |
| `beagle-callers User-name FILE` | Generated getter call sites | Lists agent B's calls |
| `beagle-provides FILE` | Module with macro | Lists generated defn/defrecord names |
| `beagle-expand FILE` | Macro invocation | Shows expanded forms |
| `beagle-check FILE` | Consumer module | Type errors if signature mismatches |

## Protocol

1. Write author module with `defentity` macro + 3 entities
2. Write consumer module importing generated functions
3. Run each query tool against both files
4. Record: does the tool resolve the generated name? (yes/no/partial)
5. Introduce a type error in the consumer (wrong field type)
6. Run `beagle-check` — does it catch the error through the expansion?

## Success criteria

All 6 queries resolve generated names. Type checking catches misuse
of generated functions. If any query fails to see through expansion,
document which and why — that's the next thing to fix.

## Files

```
experiments/e20-macro-cnf/
  DESIGN.md              — this file
  tasks/
    author.bgl           — module defining defentity macro + entities
    consumer.bgl         — module importing and using generated fns
  results/
    RESULTS.md           — query results + analysis
```
