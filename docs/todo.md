# beagle — todo

## Now: Repair compiler (fault localization + semantic properties)

### Phase 1: `beagle-blame` — runtime blame from oracle failures ✓

Post-processes verify output: analyzes expected/actual ratios to hint at
likely bug type (sign error, wrong operator, missing term, etc).

- [x] CLI: `beagle-blame <build-dir> <verify-script>`
- [x] Ratio analysis: sign inversion, multiplier mismatch, boolean flip
- [x] Confidence levels on each hint
- [ ] Deeper tracing: instrument compiled code to capture intermediate values
- [ ] Walk call graph to find first divergence point

### Phase 2: Semantic property inference (name → expected behavior) ✓

Static analysis that flags arithmetic/logic mismatches based on function
names and types. No ML, no LLM — just pattern matching.

- [x] Rule engine: name patterns → expected arithmetic direction
  - "total"/"sum" → addition/aggregation, result ≥ inputs
  - "discount" → subtraction, result < input
  - "commission"/"surcharge" → multiplication
  - "line-total"/"poline-total" → unit × quantity
  - "needed"/"required" → less-than comparison
- [x] Soft warning output (SUSPECT, never hard errors)
- [x] Integration with beagle-check-all
- [x] Aggregation context detection (don't flag + inside for/reduce)
- [x] Validated: 3 true positives, 1 false positive on E8 buggy vs golden

### Phase 3: Oracle-guided speculative fix

For each blame-traced bug, generate a candidate fix, run the oracle with
it applied, and report confidence based on whether it passes.

- [ ] Candidate generation from blame trace (arithmetic flip, accessor swap)
- [ ] Sandboxed oracle run with candidate applied
- [ ] Confidence scoring: blame evidence + name semantics + oracle pass = high
- [ ] Output as ranked repair queue (same format as beagle-fix)

## Someday

Speculative; no commitment.

- **Reader normalization pass.** The reader currently emits `#%brackets`
  tags that leak into the compiler. Add a post-read normalization step
  that resolves bracket semantics (vector vs. grouping) in one place,
  so downstream parse/check/emit never see `#%brackets` — just typed
  AST nodes. Eliminates "delimiter astrology" from the compiler core.
- **LSP / editor integration.** Type-aware completion, jump-to-def, etc.
- **Typed REPL.** Connect to a live Clojure socket-repl, evaluate
  beagle forms with full type checking before sending.

## Done

- All core forms (def, defn, fn, let, if, cond, when, do, call, vector, quote)
- try/catch/finally, doseq, case, constructor calls (ClassName.)
- defprotocol, defmulti/defmethod
- deftype, extend-type (protocol implementation on types)
- Keyword-as-function (`(:key map)`) with record field type inference
- Map literals (`{}`), set literals (`#{}`), import (Java classes)
- Map destructuring (`{:keys [a b c]}`, `{:keys [a b c] :as m}`) in params and let
- Sequential destructuring (`[a b & rest]`) in params and let
- Threading macros (`->`, `->>`) — pass-through to Clojure
- Meta: ns, define-mode, require, declare-extern, define-macro, import, unsafe
- Types: primitives, function types (incl. variadic), parametric, union, polymorphic (forall), Any
- Macros: safe (gensym-hygienic) / unsafe with &rest and splice
- Custom reader preserving `[]`/`()`, intercepting `{}`/`#{}`
- Stdlib extern catalog (~607 functions), bin/gen-stdlib-types auto-generator
- bin/beagle-build, bin/beagle-build-all, bin/beagle-expand
- 284-test suite
- experiments/ benchmark framework (40 tasks × 3 variants, gen-prompts + score + verify-behavior)
- Head-to-head benchmark (8 programs, beagle vs raw Clojure, 16/16 pass)
- Refactoring experiment (overhead-pct cascade, 2/2 pass)
- Bug detection experiment (5 injected bugs, 2/2 pass)
- loop/recur, for (with :when), sort-by language forms
- Wrapped let-binding form: `(let [(name : Type) value ...] ...)`
- Lint pass: untyped def/defn, return-type missing, unsafe escape, shadow, unused extern
- Empirical benchmark: 88 responses, 5 real bugs caught + fixed, 100% behavior pass
- Structured error output (BEAGLE_ERROR_FORMAT=json, hints, beagle-check)
- docs/findings.md empirical log
- Form catalog (docs/forms.md)
- Flow-sensitive type narrowing in if/cond/when
- Cross-file type import via (require module) / (require module :as alias)
- Polymorphic stdlib (map, filter, identity etc.)
- Atom/swap!/reset! (typed in stdlib — no special form needed)
- raco beagle build|check|expand subcommands
- Per-statement source locations for compile-time errors
- Top-level source mapping (`^{:line N :file "path"}` metadata on emitted forms)
- Expression-level source mapping (every compound form gets `^{:line N :file "path"}` metadata)
- Hygienic macros (gensym-based for safe macros)
- Cross-module defrecord import (constructors, accessors, keyword-access field types)
- v2 experiment framework (5-module inventory system, 1651 LOC, 444 assertions, 12 injected bugs)
- Type-system query tools: beagle-sig, beagle-fields, beagle-callers, beagle-provides, beagle-impact
- Clojure analog query tools: clj-sig, clj-fields, clj-callers, clj-provides
- E4 scaled experiment (13-module, 8570 LOC, 484 assertions, 35 injected bugs, first correctness divergence)
- beagle-check-all / beagle-build-all: single-process batch check/build (9-10x vs sequential)
- Rich diagnostics: Rust-style error codes, source line display, signatures, "did you mean?" suggestions
- Nullable type sugar: `String?` → `(U String Nil)`, renders back as `String?`
- Let-binding type inference (documented: always worked, agents didn't know)
- Cross-module require imports types (documented: declare-extern only needed for Java/non-beagle)
- Pattern matching (`match`): record type dispatch, positional field destructuring, map/literal/wildcard patterns
- Multi-arity `defn`: per-arity type checking, union-type call validation, proper arity error messages
- Guard-pattern type narrowing: `(when (nil? x) (throw ...))` narrows x in subsequent `do` forms
- Union-to-union type compatibility: (U A B) assignable to (U A B C) (subset check)
