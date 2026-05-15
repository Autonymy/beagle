# beagle — todo

## Next

Architecture supports adding these without API breaks.

- **Structured error output.** Errors today are formatted strings via
  `raise-syntax-error`. For LLM consumption: emit a parseable representation
  (JSON, EDN, or a structured form like `[error: category, in: file:line:col,
  expected: ..., got: ..., hint: ...]`). Lets agent runners feed precise
  feedback to the model.
- **More lint rules.** v0 lints: untyped def/defn, unsafe escape usage.
  Extend with: shadowed bindings, dead code, suspicious wrapped-vs-inline
  inconsistencies, unused imports/externs, macros with capture-prone names.
- **Per-statement source locations.** Threading syntax objects through parse
  into AST so check-time errors point at the specific form (currently they
  point at the module).
- **Hygienic macros.** v0 uses naive substitution. Move to syntax objects +
  scope marks to prevent capture.
- **Type narrowing in `if`/`cond`.** `(U String Nil)` stays `(U String Nil)`
  even after `(if (= x nil) ...)`. Flow-sensitive types needed.
- **Cross-file type info.** `(require other.beagle.module)` currently only
  emits the `:require` ns clause. Make beagle resolve and load the other
  module at compile time so types and macros are imported.
- **Polymorphic / parametric function types.** `(<T> [T -> T])` style so
  the stdlib catalog can type `map`, `reduce`, `filter` properly.
- **Add `:-` and inline-annotation parser support.** Required before
  benchmark variants B (inline) and C (`:-`) can run.
- **`raco beagle ...` subcommand registration.** So `raco beagle build`,
  `raco beagle expand`, `raco beagle test` work as proper subcommands
  rather than free-floating `bin/` scripts.

## Someday

Speculative; no commitment.

- **`#lang beaglelog`** — the graph-DB DSL built as macros on top of
  `#lang beagle`. Compiles through beagle to Clojure using Datascript /
  Datomic.
- **Local AI prose → beagle layer.** Take natural-language descriptions
  and translate them to beagle source. Local model so data stays private.
- **`#lang beagle/cljs`** — ClojureScript-targeted variant for browser
  / local-first apps.
- **Source maps proper.** `.clj.map` files so Clojure runtime errors
  point back at originating beagle source.
- **LSP / editor integration.** Type-aware completion, jump-to-def, etc.
- **Typed REPL.** Connect to a live Clojure socket-repl, evaluate
  beagle forms with full type checking before sending.

## Done in v0

- All forms (def, defn, fn, let, if, cond, when, do, call, vector, quote)
- Meta: ns, define-mode, require, declare-extern, define-macro, unsafe
- Types: primitives, function types (incl. variadic), parametric, union, Any
- Macros: safe / unsafe with &rest and splice
- Custom reader preserving `[]`/`()`
- Stdlib extern catalog (~60 functions)
- `bin/beagle-build`, `bin/beagle-build-all`, `bin/beagle-expand`
- 104-test suite
- `experiments/` benchmark framework (25 tasks × 6 variants, gen-prompts + score)
- Both `:` and `:-` annotation markers, wrapped + inline param styles
- Wrapped let-binding form: `(let [(name : Type) value ...] ...)`
- Lint pass: untyped def/defn, return-type missing, unsafe escape warnings
- First empirical benchmark sample (23 calls, 2 bugs caught + fixed)
- `docs/findings.md` empirical log
- Form catalog (`docs/forms.md`)
