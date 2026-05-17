# beagle

> follow your nose

A typed authoring layer for Clojure that minimizes repair distance for
LLM agents. Compile-time type checking, a repair compiler toolchain,
and persistent query infrastructure — all designed so AI can find and
fix bugs faster than untyped alternatives.

**LLM authoring is a first-class concern.** Rich types, explicit forms,
low syntactic surface area, structured errors. One canonical idiom per
concept. The type system is a query interface, not a proof obligation.

## Status

`#lang beagle` v0.3 — 331 tests passing, empirically validated at 8500
LOC scale.

**Key result (E4):** At 13 modules / 8570 LOC / 35 injected bugs,
beagle achieves 3/3 correctness passes vs clojure's 0/3 — the first
reproducible divergence where types produce measurably better outcomes.

## Architecture

```
source.rkt → parse → check → emit → output.clj
                ↑
      repair compiler (blame, trace, specfix, cascade)
                ↑
          daemon (persistent AST cache, 45× query speedup)
```

## What works

**Language:**
`def`, `defn` (single + multi-arity), `fn`, `let`, `if`, `cond`,
`when`, `do`, `match`, `loop`/`recur`, `for`/`doseq`, `try`/`catch`,
`case`, `defrecord`, `with` (typed record update), `defenum`,
`defprotocol`, `defmulti`/`defmethod`, `deftype`, `extend-type`,
threading (`->`, `->>`), map/set literals, keyword-as-function,
destructuring (map + sequential), `defscalar` (nominal wrappers)

**Types:**
Primitives, function types (variadic), parametric (`Vec`, `Map`, `Set`),
union, nullable sugar (`String?`), polymorphic (`forall`), user records,
nominal scalars. Flow-sensitive narrowing in `if`/`cond`/`when`.

**Stdlib:** ~607 Clojure functions pre-typed. Key HOFs polymorphic.

**Cross-module:** `(require module :as alias)` imports types, records,
constructors, accessors, macros. All validated at call sites.

**Diagnostics:** Rust-style error display with source lines, signatures,
"did you mean?" suggestions. JSON mode for zero-tool-call bug fixes.

## Repair compiler

The repair compiler closes the loop: agent writes code → evidence
system produces a ranked repair queue → agent applies fixes → done.

| Tool | Purpose | Speed |
|------|---------|-------|
| `beagle-repair` | Unified pipeline: type errors + blame + specfix → ranked queue | ~5s with daemon |
| `beagle-trace` | Per-assertion arithmetic trace — exact divergence point | ~0.5s/assertion |
| `beagle-specfix` | Oracle-guided candidate fixes (verified, not suggested) | ~5s with bb |
| `beagle-cascade` | Call graph impact — find root causes, not symptoms | <1s |
| `beagle-blame` | Ratio analysis: sign error, wrong operator, missing term | <1s |
| `beagle-oracle` | Generate oracle from golden code (golden = test spec) | ~0.5s with bb |

## Query tools (daemon-accelerated)

The type system is a query interface for agents:

```bash
beagle-sig order-total .           # [Order -> Amount]
beagle-fields Invoice .            # id, order-id, total, status...
beagle-callers order-total .       # all call sites + arg counts
beagle-provides billing.rkt        # full module export list
beagle-impact order-total .        # callers + downstream effects
```

With daemon running: 10ms per query (vs 450ms cold).

## Daemon

`beagle-daemon start` launches a persistent Racket process that caches
parsed ASTs with mtime invalidation. Query tools transparently route
through it when running, fall back to cold Racket when not.

```bash
beagle-daemon start     # TCP server, ephemeral port
beagle-daemon status    # JSON: cached file count, uptime
beagle-daemon stop      # graceful shutdown
```

Oracle runs use Babashka (`bb`) instead of JVM Clojure: 0.18s vs 2.14s
for 484 assertions.

## Build & check

```bash
bin/beagle-build-all *.rkt --out .build/   # batch compile (9× vs sequential)
bin/beagle-check-all .                      # batch type-check (10× vs sequential)
bin/beagle-build source.rkt [out.clj]       # single file
bin/beagle-check source.rkt                 # type-check only
bin/beagle-expand source.rkt                # post-macro expansion
```

## A sample program

```racket
#lang beagle
(ns inventory.core)
(define-mode strict)
(require catalog :as cat)

(defscalar WarehouseId Long)

(defrecord StockLevel [(warehouse-id : WarehouseId)
                       (product-id : cat/ProductId)
                       (quantity : Long)
                       (min-quantity : Long)])

(defn understocked? [(s : StockLevel)] : Boolean
  (< (stocklevel-quantity s) (stocklevel-min-quantity s)))

(defn reorder-quantity [(s : StockLevel)] : Long
  (if (understocked? s)
      (- (stocklevel-min-quantity s) (stocklevel-quantity s))
      0))
```

## Escape hatches

1. `(unsafe "raw clojure")` — literal Clojure string
2. `(define-macro unsafe ...)` — macro expansion typed as `Any`
3. `(define-mode dynamic)` — skip type checking for a file
4. Hand-written `.clj` — the module boundary is an escape hatch

## Setup

Requires [Racket](https://racket-lang.org/) and [Babashka](https://babashka.org/).

```
raco pkg install --link --auto /path/to/beagle
```

## Tests

```
raco test tests/
```

## Reference

- `docs/cheatsheet.md` — single-page LLM-grounding reference
- `docs/agent-workflow.md` — decision tree for repair tool routing
- `docs/forms.md` — canonical form catalog
- `docs/todo.md` — roadmap and completed work
- `docs/devlog/` — development journal (discoveries + experiments)
- `experiments/` — benchmark framework (E1–E9)
