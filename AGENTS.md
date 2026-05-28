# beagle — agent instructions

## What this is

A typed authoring IR for Nix. `#lang beagle/nix` is the live target;
five other backends (Clj, CLJS, JS, Py, Rkt, SQL) are parked under
`beagle-lib/private/dormant/` and reactivate with
`BEAGLE_ALL_TARGETS=1`. Pipeline: parse → check → emit, all at Racket
expand-time.

The deeper session anchor is `CLAUDE.md`. Read that first for anything
nontrivial. AGENTS.md is a quick map for tools that don't honor
`CLAUDE.md`.

## Package layout

```
beagle-lib/                          # collection "beagle" — compiler + stdlib
beagle-lib/private/                  # core: parse, check, emit
beagle-lib/private/dormant/          # parked non-Nix emitters + stdlibs
beagle-test/                         # collection "beagle" — tests
beagle/                              # multi-collection aggregate
```

## How to test

```
bin/beagle-test                                       # Nix-tier default loop
BEAGLE_ALL_TARGETS=1 bin/beagle-test                  # + dormant target tests
raco test beagle-test/tests/parse.rkt                 # just parser
raco test beagle-test/tests/emit-nix.rkt              # just Nix emitter
raco test beagle-test/tests/check.rkt                 # just type checker
```

End-to-end:
```
bin/beagle-op-check path/to/file.bnix   # operative-pipeline check
bin/beagle-op-compile path/to/file.bnix # operative-pipeline compile
bin/beagle-build path/to/file.bnix      # legacy pipeline (still works)
```

## How to add a new form

1. **Struct** in `beagle-lib/private/ast.rkt` — `(struct name (fields) #:transparent)`; add to the provide list
2. **Parse case** in `parse-list-form` (in `parse.rkt`) — pattern-match source into the struct
3. **Emit case** in `beagle-lib/private/emit-nix.rkt` — produce Nix source. Don't touch `dormant/emit-{clj,js,py,rkt,sql}.rkt` unless you are actively reactivating that target.
4. **Infer case** in `beagle-lib/private/check.rkt` `infer-expr` — return a type (`ANY` if unknown)
5. **Lint traversal** in `beagle-lib/private/lint.rkt` — `check-shadow` and `collect-symbols`
6. **Tests** in `beagle-test/tests/parse.rkt`, `emit-nix.rkt`, `check.rkt`

## Surface (role-locality + public-contracts)

Current surface uses head-tagged structural sub-lists. Quick reference:

```racket
(defn add (params (x : Int) (y : Int)) (+ x y))
(let (<- x 1 y 2) (+ x y))
(defrecord Point (fields (x : Int) (y : Int)))
(claim withdraw :type (-> (param amount Int (where (> amount 0))) Int))
(claim Person (record (field name String) (field age Int)) (where (>= age 0)))
```

`'` is reserved for inert data (paths, code-as-data): `(at cfg (' :a :b :c))`.

## Test helpers

The current surface writes directly in tests — no bracket-tag tricks:

```racket
(parse-one '(defn foo (params (x : Int)) (+ x 1)))
(parse-one '(let (<- x 1) x))
```

`(br …)` / `(mp …)` helpers are still defined for migrator tests
exercising pre-tightening v0.15 inputs.

## Key file map

| file | role |
|---|---|
| `beagle-lib/lang/reader.rkt` | Custom reader: `[]`, `{}`, `#{}`, `#"..."` |
| `beagle-lib/private/parse.rkt` | Source → AST |
| `beagle-lib/private/check.rkt` | Legacy-pipeline type checker |
| `beagle-lib/private/check-operative.rkt` | Operative-pipeline checker (refinements, etc.) |
| `beagle-lib/private/emit-nix.rkt` | AST → Nix source |
| `beagle-lib/private/emit-operative.rkt` | Operative-pipeline Nix emitter |
| `beagle-lib/private/types.rkt` | Type AST, MAP-TAG/SET-TAG, compatibility |
| `beagle-lib/private/stdlib-nix.rkt` + `stdlib-portable.rkt` | Live stdlib catalogs |
| `beagle-lib/private/dormant/stdlib-{clj,cljs,js,py,sql}.rkt` | Parked stdlib catalogs |
| `beagle-lib/private/lint.rkt` | Shadow detection, unused externs, untyped warnings |
| `beagle-lib/private/macros.rkt` | Macro registry (template `safe` + procedural `proc`/`beagle`) |
| `beagle-lib/main.rkt` | `#%module-begin` — full pipeline |

## Important conventions

- `ANY` is `(type-prim 'Any)` — the universal escape type
- `MAP-TAG` and `SET-TAG` are `'#%map` and `'#%set` (well-known symbols, NOT gensyms — gensyms break across Racket phase boundaries)
- The reader runs at phase 0, the parser at phase 1 (inside `define-syntax`) — shared symbols must be phase-stable
- `emit-form` handles top-level forms (def, defn, defrecord, defunion, defenum); `emit-expr` handles everything else
- Params can be `param`, `map-destructure`, or `seq-destructure` structs — always check with `(map-destructure? p)` / `(seq-destructure? p)` before calling `(param-name p)`

## What NOT to do

- Don't recreate `docs/` in beagle. Papers go in `~/code/life-os/threads/`.
- Don't add forms whose emit only lands in dormant/ — the new form simply won't exist there until that target is reactivated, which is fine.
- Don't add type aliases (e.g. `Long` for `Int`) — removed by design.
- Don't add `#(...)` fn shorthand — cargo-culted out.
- Don't use gensyms for reader tags — they break across phases.
- Don't skip lint traversal when adding forms.
- Don't write `unsafe-*` anything. There are zero escape hatches.
