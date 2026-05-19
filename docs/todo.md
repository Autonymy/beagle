# beagle — todo

## Now: Developer experience

### MCP server ✓

Expose beagle's query tools over Model Context Protocol so any agent
framework gets type-aware code intelligence without beagle-specific wiring.

- [x] MCP server binary (`bin/beagle-mcp` / `beagle mcp`)
- [x] Tools: `sig`, `fields`, `callers`, `provides`, `impact`
- [x] Tool: `check` — type-check a file, return structured diagnostics
- [x] Tool: `build` — compile a file, return Clojure source or errors
- [x] Daemon integration: delegate to running daemon for speed
- [x] stdio transport (standard MCP)

### `beagle init --claude-code` ✓

One command wires up everything for Claude Code: daemon, hooks, system prompt.

- [x] `--claude-code` flag on existing `beagle init`
- [x] Generate `.claude/settings.json` with PostToolUse hook (daemon diagnostics on .rkt edit/write)
- [x] Generate `.claude/hooks/beagle-check.sh` — daemon-first, CLI fallback
- [x] Generate `CLAUDE.md` snippet with beagle context (from consumer cheatsheet)
- [x] Print setup summary (what was created, how to start daemon)

## Open

### JS target gaps

- [x] `set!` for property mutation — `(set! (.-value el) "")` parsed and emitted for CLJ + JS
- [x] ~45 stdlib fns in `emit-core-call`: mapv, filterv, sort-by, dissoc, update, merge, get, subvec, pop, peek, some, take, drop, vector?, map?, distinct, flatten, complement, constantly, partial, comp, frequencies, group-by, partition, interleave, juxt, not-empty, take-last, drop-last, sequential?, seq?, coll?, set?, pr-str, to-array, aget, aset, array-seq, clj->js, js->clj, seq, not=
- [x] Bare npm imports — single-word requires emit as bare package imports
- [x] `letfn` — mutual recursion local fns (CLJ + JS emit, lint, 550 tests)
- [x] Atom ops in emit-core-call — `atom`, `deref`, `reset!`, `swap!`, `add-watch`, `remove-watch`
- [x] Core fns as higher-order values — JS-VALUE-WRAPPERS emit lambda wrappers in value position; binding-aware (user defs shadow stdlib)
- [x] JS-NO-EMIT safety net — compile-time warning for portable stdlib fns with no JS translation (139 symbols)
- [x] `beagle.core.js` runtime — 12 finite helpers: range, remove, mapcat, every?, keep, map-indexed, assoc-in, update-in, select-keys, merge-with, take-while, drop-while
- [x] STDLIB-JS — 38 JS-native type declarations (Math, JSON, Promise, fetch, timers, Object, Array, console)
- [x] `beagle-js-coverage` — coverage report showing `silent fallback: 0`

### Doc consolidation

- [x] Delete dead weight: `forms.md`, `cheatsheet-distilled.md`, `findings.md`, prompts stubs
- [ ] Strip CLAUDE.md experiment results into experiments/report.md only
- [ ] Single cheatsheet generation from Scribble

### Doc generation / single source of truth

- [x] Extend `beagle-docs-sync` to propagate canonical type names from `private/types.rkt`
- [x] Add `CLAUDE.md` instructions to use `beagle-docs-sync` after type/form changes
- [ ] Scribble as single source → generate markdown cheatsheets from Scribble docs
- [ ] Template markers in markdown docs (`{{types}}`, `{{example}}`) expanded by docs-sync
- [ ] Canonical example program in one place, referenced by README/cheatsheet/consumer docs

### Proper packaging

Package beagle as a proper Racket package so it can be installed via `raco pkg install`
from the catalog (not just `--link`).

- [x] Add `info.rkt` with proper deps, collection, pkg metadata, tags, test-paths
- [ ] Follow [Racket package tutorial](https://blog.racket-lang.org/2017/10/tutorial-creating-a-package.html)
- [ ] Register on [Racket package catalog](https://github.com/racket/racket/wiki/Creating-Packages)

### Nix target: full Nisp parity

`beagle/nix` should express real NixOS modules without `unsafe` escape hatches.
All forms are target-specific (`nix-*` AST nodes, invalid outside `beagle/nix`).

**Phase 1 — Module-writing core:**
- [x] Core emitter (defn→curried, defrecord→mkType, maps→attrsets, etc.)
- [x] stdlib-nix (120 typed entries: builtins/lib/lib.types)
- [x] `fn-set` / `fn-set-rest` / `fn-set@` — attrset-pattern lambda
- [x] `inh` / `inh-from` — inherit
- [x] `with-do` — with scope injection
- [x] `s` — string interpolation
- [x] `p` — path literals

**Phase 2 — Nix semantic essentials:**
- [x] `rec-att` — recursive attrsets
- [x] `assert-do` — assert expression
- [x] `get-or` — select with or-default
- [x] `has` — has-attr check
- [x] `ms` — multiline strings
- [x] `spath` — search path literals

**Phase 3 — Operator/convenience parity:**
- [x] `pipe-to` / `pipe-from` — pipe operators
- [x] `impl` — logical implication

**Phase 4 — Target-form gating:**
- [x] `TARGET-ONLY-FORMS` registry in `check.rkt` — compile error for target-specific forms outside their target
- [x] `check-target-form` validation in `infer-expr` — gates all 15 nix forms + `await`
- [x] Fix `emit-nix.rkt` silent `await` handling (was emitting inner expr, now errors)
- [x] Cross-target rejection tests (10 tests: await on clj/nix, nix forms on clj/js)

Win condition: every NixOS module expressible in Nisp is expressible
in `beagle/nix` without `unsafe`. Emitted Nix evaluates correctly.

### Experiment metadata

- [x] Add version + dialect table to `experiments/report.md` (v0.1–v0.5, all `#lang beagle` / Clojure target)
- [ ] E13 confound isolation: full prompt vs cheatsheet, daemon vs no daemon

## Completed

<details>
<summary>v0.1–v0.6.1 (click to expand)</summary>

### Repair compiler (phases 1–5)

- beagle-blame: ratio analysis, confidence levels, call-graph tracing
- beagle-specfix: 9 candidate strategies, accessor swap, arg permutation, cross-evidence correlation
- beagle-trace: per-assertion arithmetic trace, source location correlation, call-graph walk
- beagle-cascade: call graph impact, predictive blame, root cause detection
- beagle-repair: unified pipeline, --auto mode, --emit-patch (unified diff output)

### Property testing & oracles (phases 6–8)

- beagle-proptest: record generators, return-type property inference, differential testing, shrinking
- beagle-oracle: golden snapshot, assertion generation, differential mode
- beagle-muttest: 13 mutation operators, coverage gap reports

### Infrastructure

- LSP server: hover, diagnostics, document symbols, jump-to-definition, completion
- Typed REPL: persistent env, :type/:sig/:env, daemon integration
- Reactive daemon: file watcher, ~100ms re-check, 45× query speedup
- Distributed tracing: beagle-dtrace (instrument, collect, view, blame, graph, cascade)
- CLJS target: JS interop, source maps, shadow-cljs validated (Heist 40/40)
- Refinement predicates: compile-time literal checking + runtime :pre
- Query tools: beagle-sig, beagle-fields, beagle-callers, beagle-provides, beagle-impact

### Releases

- v0.4.0: unified CLI, consumer cheatsheet, error message audit, type checker hardening
- v0.5.0: docs/prompts/, nix flake, beagle-docs-sync, README update
- v0.6.0: form completeness (when-not, if-not, condp, dotimes, defonce, comment), Scribble docs
- v0.6.1: Scribble polish

### Experiments

- E1–E3: initial benchmarks (8 programs, refactoring, bug detection)
- E4: scaled experiment (13 modules, 8570 LOC, 35 bugs — first correctness divergence)
- E5: event-sourced pipeline (8 modules, 40 bugs)
- E9: repair toolchain (29% faster, 36% fewer tokens)
- E10: workflow compression (33% faster wall time)
- E11: model tier (Opus 33% gain, Sonnet 4%, Haiku 2%)
- E12: Python gap analysis + clj-kondo track
- E13: reactive daemon (287s avg, per-bug faster than Python+mypy)
- E14–E15: multi-agent pool (abandoned — 0 activations across 7 runs)

### Language

471 tests. ~678 stdlib entries. All core Clojure forms implemented.
Pattern matching, multi-arity defn, guard narrowing, union types,
cross-module import, macros (safe/unsafe), defrecord/defscalar/defenum/defunion,
destructuring, threading, Java interop, metadata, for/doseq/dotimes,
try/catch, loop/recur, all conditional forms.

</details>
