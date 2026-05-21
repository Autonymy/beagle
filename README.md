# Beagle

A typed Lisp authoring layer for agent-written code. Agents write typed, structural source. Beagle checks it, repairs it, and emits ordinary Clojure / JavaScript / Nix / Python / SQL — or Typed Racket for independent type verification.

**The types are scaffolding. The emitted code is the building.**

Beagle checks *generation*, not runtime — emitted code carries no type guards by design. The goal is to catch the mechanical errors agents make while writing (wrong fields, missing cases, invalid interop), then get out of the way.

```text
source.bclj/.bjs/.bnix/.bpy → parse → check → emit → .clj / .js / .nix / .py
                             ↑
                    repair compiler
                             ↑
                      daemon + AST cache
```

## Core ideas

- **S-expressions as structural compression.** Source is close to an AST — repair tooling is cheap and structural, not expensive and string-based.
- **Explicit mutation.** Agents over-explore mutation-coupled programs. Beagle marks mutation explicitly, collapsing the reasoning search space.
- **Authoring-time types.** Types catch mechanical errors during generation — wrong fields, missing cases, invalid interop. Then they disappear.
- **Dynamic runtimes as targets.** One typed surface, multiple backends. Clojure stays Clojure. JavaScript stays JavaScript.

## Targets

- `#lang beagle/clj` — Clojure
- `#lang beagle/cljs` — ClojureScript
- `#lang beagle/js` — JavaScript
- `#lang beagle/nix` — Nix
- `#lang beagle/py` — Python
- `#lang beagle/sql` — SQL *(experimental)*
- `#lang beagle/rkt` — Typed Racket *(oracle — validates type promises via `raco make`)*

## A program

```racket
#lang beagle/js
(ns inventory.core)
(define-mode strict)

(defrecord StockLevel [(product-id : Int)
                       (quantity   : Int)
                       (min-qty    : Int)])

(defn understocked? [(s : StockLevel)] : Bool
  (< (stocklevel-quantity s) (stocklevel-min-qty s)))

(defn reorder-quantity [(s : StockLevel)] : Int
  (if (understocked? s)
      (- (stocklevel-min-qty s) (stocklevel-quantity s))
      0))
```

## Procedural macros

Compile-time code generation with typed AST contracts. The macro body is Racket; inputs and outputs are contract-checked; the expansion goes through the full type-checking pipeline.

```racket
#lang beagle
(define-macro proc defentity
  [(name : Symbol) (fields : (Vec Syntax))] : (Vec Form)
  (cons
    `(defrecord ,name ,(map (lambda (f) (list (car f) ': (caddr f))) fields))
    (map (lambda (f)
           `(defn ,(string->symbol (format "~a-~a" name (car f)))
              ((r : ,name)) : ,(caddr f)
              (get r ,(string->symbol (format ":~a" (car f))))))
         fields)))

(defentity User ((name : String) (email : String) (age : Int)))
;; → defrecord User + typed getters User-name, User-email, User-age
```

Template macros can't express this — they can't iterate over data to generate variable numbers of forms. Proc macros compress 2-3× at realistic scale (E18).

## Experiments

### E16: Does the type checker make agents faster?

4 features built by Claude Sonnet agents — one group with no type checker, one with Beagle's structural checker (n=4, treat as directional):

| | No types | With types | |
|---|---:|---:|---|
| Avg build time | 362s | **274s** | **24% faster** |
| Correctness | 8/8 | 8/8 | identical |
| Hardest feature | 600s | **328s** | **45% faster** |

Types didn't move correctness at this scale — they moved how fast the agent got there, with the gap widening on features with more coordination complexity.

The load-bearing finding is about *integration*, not the checker itself: the same checker, poorly wired into the agent loop (noisy output, wrong workflow position, vague framing), imposed a 76% penalty. Three non-code fixes swung the outcome by 100 percentage points. The contribution is as much *how* the checker reaches the agent as the checker.

[Results](experiments/e16-workflow-scheduler/results/type/RESULTS.md) · [Devlog](docs/devlog/018-e16-type-surface.md)

### E18–E19: Procedural macros

E18 measured compression: proc macros compress 2-3× at realistic scale (crossover at 2-4 instances). Template macros can't express any of the three test patterns — all require iterating over data.

E19 tested agent authoring: a prompted agent (with proc macro docs) wrote a working macro in 2 iterations / 271s. An unprompted agent (no docs) independently invented runtime data dispatch in 1 iteration / 117s — the structurally correct fallback, but without compile-time type coverage.

Key finding: proc macro docs are load-bearing for discoverability. Without them, agents reach for runtime patterns.

[E18 Results](experiments/e18-macro-compression/results/RESULTS.md) · [E19 Results](experiments/e19-agent-macro-authoring/results/RESULTS.md)

### E1–E15: Cross-language comparison

| Metric                    | Beagle | Clojure | Python + mypy |
| ------------------------- | -----: | ------: | ------------: |
| Correctness (E4, 35 bugs) |    3/3 |     0/3 |           3/3 |
| Best wall time            |   287s |    365s |          255s |

Beagle matches the typed baseline (mypy) on correctness and beats the untyped one (Clojure). mypy edges wall time — the trade Beagle makes is one typed surface across multiple backends, not single-language speed.

[Full methodology](experiments/report.md)

## Setup

Requires [Racket](https://racket-lang.org/) 8.x+.

```sh
raco pkg install beagle
```

Or from source:

```sh
raco pkg install --link beagle-lib/ beagle-test/ beagle-doc/ beagle/
raco test beagle-test/tests/   # 1221 tests
```

## Agent integration

```sh
beagle init --claude-code
beagle-daemon start --watch .
```

Generates a PostToolUse hook, settings, `CLAUDE.md`, and language context. The daemon re-checks within ~100ms of each save.

## Tooling

- **LSP server** — hover, diagnostics, symbols, jump-to-definition, completion
- **Typed REPL** — persistent environment, parse → check → emit per input
- **Reactive daemon** — AST cache, inotify file watching, ~100ms re-check
- **Repair compiler** — blame, specfix, trace, cascade analysis
- **Property testing** — record generators, return-type inference, differential testing

## Documentation

- [`docs/cheatsheet.md`](docs/cheatsheet.md) — language summary
- [`docs/agent-workflow.md`](docs/agent-workflow.md) — repair tool routing
- [`docs/tool-reference.md`](docs/tool-reference.md) — CLI and tool catalog
- [`docs/devlog/`](docs/devlog/) — development journal (21 entries)
- [`experiments/report.md`](experiments/report.md) — E1–E15 results

## Related

Beagle is a language bridge for [Claim Normal Form](https://github.com/tompassarelli/claim-normal-form) — its typed forms map into CNF's entity/claim graph.
