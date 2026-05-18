# beagle

A language where the compiler does the debugging.

Beagle is an agent-native language: a typed authoring layer targeting
Clojure/ClojureScript, designed to minimize agent repair distance.
Racket frontend, custom `#lang`, static type checking — emits plain
`.clj` / `.cljs` for runtime. The language exists because the repair
loop needs structured evidence.

## Thesis

Mechanical bugs should not require cognition. They should compile into
patches.

Beagle turns debugging from reasoning work into patch-application work.
Types catch shape errors at compile time. A repair compiler turns
runtime failures into ranked, machine-actionable fix candidates — then
emits them as executable patches. Zero reasoning tokens on mechanical
fixes; the agent's budget is spent entirely on semantic bugs that
require judgment.

## Evidence

Fifteen experiments (E1–E15) across three language tracks (Beagle,
Clojure, Python), head-to-head on the same tasks.

**E4** (13 modules, 8570 LOC, 35 injected bugs): beagle 3/3
correctness vs clojure 0/3. First reproducible divergence — but this
is a static-typing result, not a beagle result: Python + mypy also
achieves 3/3.

**E9** (repair toolchain): beagle gives the agent a better repair
queue. 29% faster, 36% fewer tokens, same correctness.

**E13** (reactive daemon): inotify watcher re-checks every file within
~100ms of each save. Enriched diagnostics injected after every edit.
287s avg — variance collapsed from 142s range to 59s. Per-bug faster
than Python + mypy (8.2s vs 8.5s). The best single-agent configuration.

**Python + mypy** (same system, typed dataclasses): 255s avg — fastest
absolute track. But per-bug, beagle E13 is faster. The gap is narrowing.

**E14–E15** (multi-agent pool): agents currently resist multi-agent edit
delegation in ways that make this an impractical optimization target.
Four approaches tested, zero activations. Abandoned.

**Within Clojure:** beagle + reactive daemon (287s) beats the best
Clojure configuration (365s with clj-kondo) by 21%.

Full methodology and results: [`experiments/report.md`](experiments/report.md)

## Architecture

```
source.rkt → parse → check → emit → output.clj
                       ↑
             repair compiler (blame, trace, specfix, cascade)
                       ↑
                 daemon (persistent AST cache, 45× query speedup)
```

- `lang/reader.rkt` — custom reader preserving `[]` vs `()`
- `private/parse.rkt` — source → AST (two-pass: meta collection, then exprs)
- `private/check.rkt` — type checking, record fields, flow narrowing
- `private/emit.rkt` — AST → qualified Clojure source with source maps
- `private/daemon.rkt` — TCP server, AST cache with mtime invalidation

Plain `#lang racket/base` throughout — beagle implements its own type
system rather than using Typed Racket.

## A program

```racket
#lang beagle
(ns inventory.core)
(define-mode strict)
(require catalog :as cat)

(defrecord StockLevel [(product-id : Long)
                       (quantity   : Long)
                       (min-qty   : Long)])

(defn understocked? [(s : StockLevel)] : Boolean
  (< (stocklevel-quantity s) (stocklevel-min-qty s)))

(defn reorder-quantity [(s : StockLevel)] : Long
  (if (understocked? s)
      (- (stocklevel-min-qty s) (stocklevel-quantity s))
      0))
```

## Language

**Forms:** `def`, `defn` (single + multi-arity, varargs with `&`),
`fn`, `let`, `if`, `cond`, `when`, `do`, `match`, `loop`/`recur`,
`for`/`doseq`, `try`/`catch`, `case`, `defrecord`, `with` (typed
record update), `defenum`, `defunion`, `defprotocol`, `defmulti`/`defmethod`,
`deftype`, `extend-type`, `defscalar`, threading (`->`, `->>`),
map/set literals, keyword-as-function, destructuring

**Types:** primitives (`String`, `Long`, `Double`, `Boolean`,
`Keyword`, `Symbol`, `Nil`, `Any`), function types (variadic),
parametric (`Vec`, `Map`, `Set`), union (`U`), nullable (`String?`),
polymorphic (`forall`), user records, nominal scalars

**Cross-module:** `(require module :as alias)` imports types, records,
constructors, accessors, macros — all validated at call sites

**Stdlib:** ~666 Clojure functions pre-typed, key HOFs polymorphic

**Diagnostics:** Rust-style errors with source lines, signatures,
"did you mean?" suggestions; JSON mode for programmatic consumption

## Repair compiler

The compiler is part of the agent's motor cortex: agent writes code →
type checker catches shape errors → repair compiler ranks and patches
mechanical fixes → agent spends its budget on semantic bugs only.

| Tool | What it does |
|------|-------------|
| `beagle-repair` | Unified pipeline: type errors + blame + specfix → ranked queue |
| `beagle-trace` | Per-assertion arithmetic trace — exact divergence point |
| `beagle-specfix` | Oracle-guided candidate fixes (verified, not suggested) |
| `beagle-cascade` | Call graph impact — find root causes, not symptoms |
| `beagle-blame` | Ratio analysis: sign error, wrong operator, missing term |
| `beagle-oracle` | Behavioral oracle from golden code (golden = test spec) |

## Query tools

The type system is a query interface, not just a proof obligation.
With daemon running: 10ms per query (vs 450ms cold).

```bash
beagle-sig order-total .           # [Order -> Amount]
beagle-fields Invoice .            # typed fields + accessors
beagle-callers order-total .       # all call sites + arg counts
beagle-provides billing.rkt        # full module export list
beagle-impact order-total .        # callers + downstream effects
```

```bash
beagle-daemon start     # persistent TCP server, ephemeral port
beagle-daemon status    # cached file count, uptime
beagle-daemon stop      # graceful shutdown
```

## Build & check

```bash
beagle-build-all *.rkt --out .build/   # batch compile (9×)
beagle-check-all .                     # batch type-check (10×)
beagle-build source.rkt [out.clj]      # single file
beagle-check source.rkt                # type-check only
beagle-expand source.rkt               # post-macro expansion
```

Oracle runs use Babashka for 12× speedup over JVM Clojure.

## Escape hatches

1. `(unsafe "raw clojure")` — literal Clojure, top-level or expression
2. `(define-macro unsafe ...)` — macro expansion typed as `Any`
3. `(define-mode dynamic)` — skip type checking for a file
4. `--warn` flag — emit despite type errors (annotate, don't block)

## Setup

Requires [Racket](https://racket-lang.org/) and
[Babashka](https://babashka.org/).

```
raco pkg install --link --auto /path/to/beagle
raco test tests/   # 399 tests
```

## Prompts

Pre-built system prompts for agents working with beagle code.

| Prompt | Audience | Use |
|--------|----------|-----|
| `docs/prompts/consumers/full.md` | Agents writing beagle code | Full language reference — load as system context |
| `docs/prompts/consumers/distilled.md` | Agents who know Clojure | Clojure-delta — only what's different |
| `docs/prompts/contributors/src.md` | Agents modifying the compiler | Architecture, conventions, file map |

Consumer prompts ship with `beagle init`. Contributor knowledge feeds
into `CLAUDE.md` (auto-loaded by Claude Code) and `AGENTS.md`.

Mechanical facts (test count, stdlib size, devlog count) are propagated
by `bin/beagle-docs-sync` — run it after changes that affect these numbers.

## Reference

- `docs/cheatsheet.md` — full language reference (LLM system context)
- `docs/cheatsheet-consumer.md` — compact consumer reference (used by `beagle init`)
- `docs/agent-workflow.md` — repair tool routing decision tree
- `docs/forms.md` — canonical form catalog with examples
- `docs/prompts/` — pre-built agent system prompts (consumer + contributor)
- `docs/devlog/` — development journal, 17 entries
- `experiments/report.md` — E1–E15 methodology and results
