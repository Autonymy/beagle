---
status: active
priority: 1
---

# Cyclone self-hosting — beagle on Scheme, Racket-free

## Thesis

Beagle's current compiler runs on Racket. The end state is: beagle compiles
itself, runs on Cyclone Scheme, no Racket dependency for end users. This
moves beagle from "Racket-hosted compiler that emits to N targets" to
"self-hosted typed authoring language with Cyclone Scheme as its native
runtime."

## Why Cyclone

After evaluating Chez, Gambit, CHICKEN, Loko, and Cyclone:

- **Architectural alignment.** Cyclone's "many small passes" pipeline is
  exactly beagle's (parse → check → emit-dispatch → emit-X). AST-as-S-expr
  is beagle's existing model. The mental-model overhead during port is
  effectively zero.
- **R7RS primary.** Cleaner module system, modern exceptions, better
  records than R6RS or R5RS-with-retrofits. Forward-looking.
- **Concurrent GC + native threads.** No stop-the-world pauses. Beagle's
  repair-agent pool, daemon, LSP server all benefit. Subprocess-forked
  Racket instances become shared-heap native threads.
- **Macro fit.** Beagle's `define-macro beagle` procedural macros map to
  Cyclone's explicit-renaming macros almost without translation.
- **Compile-to-C.** Static binary distribution, easy embedding, FFI to C
  is native (matters for inotify, sqlite, libuv-style I/O).
- **Chibi lineage.** Cyclone reused proven code (lexer, syntax-rules, heap
  data structures) from Chibi — the small/reliable R7RS reference impl.
- **Aesthetic alignment.** beagle is a focused modern tool; Cyclone is a
  focused modern Scheme. Same character, same trajectory.

Gambit was the close runner-up (faster, more mature, Termite for actor
concurrency). Cyclone wins on R7RS primacy + concurrent GC + designed-
for-compiler-substrate intent. The 6× performance gap from Cyclone's 2017
benchmarks has narrowed substantially; for a compiler in a watch loop the
difference is functionally invisible.

Chez (Racket's substrate) was the obvious establishment pick. But Racket
needs Chez because Racket is a *language workbench*; beagle is a focused
compiler. Different needs.

## Design

### Default target change

`.bgl` (the primary extension, previously target-neutral with implicit
Clojure default) now means **Cyclone Scheme target**. This is the honest
default: if beagle's compiler itself runs on Cyclone, then "just beagle"
should mean "Cyclone."

- `#lang beagle` (no qualifier) → target Cyclone Scheme.
- `#lang beagle/clj` etc. unchanged — explicit qualifier overrides default.
- Other extensions unchanged: `.bclj` / `.bjs` / `.bnix` / `.bpy` / `.bsql` / `.brkt`.
- The "portable subset" stays a real property (files using only
  `stdlib-portable.rkt` can be re-emitted to any target), but the *default*
  emit target shifts from Clojure to Scheme.

### Files

- `beagle-lib/private/emit-scheme.rkt` — AST → R7RS source string. Smallest
  emit module of any target (Scheme is near-identity to beagle's surface).
- `beagle-lib/private/stdlib-scheme.rkt` — R7RS + SRFI bindings. Small
  catalog; most beagle stdlib maps to R7RS directly.
- `beagle-lib/scheme/main.rkt` — target module (`#lang beagle/scheme`).
- `beagle-lib/scheme/lang/reader.rkt` — reader hook.
- Wire into `beagle-lib/private/emit-dispatch.rkt`.

### Tests

- `.bgl` and `.bscm` fixtures (the latter for explicit `#lang beagle/scheme`).
- `beagle-test/tests/scheme-fixtures.rkt` — compile each fixture, run
  through `cyclone` (or `icyc` interpreter), assert output.
- `beagle-test/tests/scheme-exec-oracle.rkt` — execution oracle (compile +
  run, validate behavior), matching the py/js-exec-oracle pattern.

## Phasing

### Phase 1 — emit-scheme + minimal stdlib + fixtures

Status: in progress.

- Add `emit-scheme.rkt` covering: `def`, `defn`, `defrecord`, `defunion`,
  `let`, `if`, `cond`, `fn`, `for`, `match`, `case`, primitive arithmetic,
  string ops, list/vec/map/set operations.
- Add `stdlib-scheme.rkt` with R7RS + SRFI 1/13/69/115 bindings for
  list/string/hash/regex.
- Add target module + reader hook.
- ~8 fixtures covering the form catalog above.
- Execution oracle confirming Cyclone runs the emitted code with expected
  output.

Acceptance: full `raco test beagle-test/tests/` still passes; new
scheme-fixtures tests pass; Cyclone successfully compiles + runs each
fixture.

### Phase 2 — self-host port

Status: queued.

- Inspect `self-host/*.bjs` for target coupling. Some look JS-specific
  (`(declare-extern JSON Any)` etc.); others should be portable.
- For each self-host module:
  - If portable: rename to `.bgl` (now defaults to Scheme). Single source,
    emits to both JS and Scheme.
  - If genuinely JS-coupled: create `.bscm` sibling, share what's portable.
- Build a `self-host/build-scheme.sh` that compiles each self-host module
  to Scheme and produces a working compiler binary.
- Add `self-host/verify-bootstrap-scheme.sh` — differential check between
  the Racket beagle and the Scheme-compiled beagle on the full fixture
  corpus.

Acceptance: Scheme-compiled beagle produces byte-identical output to
Racket beagle across the entire fixture suite.

### Phase 3 — bootstrap closure & decommission Racket dependency

Status: future.

- Once Scheme-beagle is verified equivalent, end users can install Cyclone
  + beagle source and bootstrap without Racket.
- Racket remains a *development* dependency (for the bootstrap chain) but
  not a *runtime* dependency.
- Documentation: install path for end users becomes "install Cyclone,
  `make bootstrap`, done."

### Phase 4 — Racket-free build chain

Status: speculative.

- Drop Racket entirely. Scheme-beagle compiles itself from `.bgl` source
  without ever invoking Racket.
- This requires the Scheme-beagle to be capable enough to compile the
  full beagle codebase (including macro-eval, all targets, all forms).
- Probably blocked on Phase 2 revealing edge cases that Phase 1 didn't
  exercise.

## Risks

- **Macro-eval port complexity.** `macro-eval.rkt` is a compile-time
  beagle interpreter. Cyclone has eval and ER macros; the port should be
  mechanical but until we try it we don't know what surprises lurk.
- **Cross-platform Cyclone install.** macOS/Linux are well-supported.
  Windows works but is less tested. For the NixOS user base, this is fine.
- **Performance on real workloads.** Synthetic benchmarks are 6×-ish
  slower than Gambit in 2017 numbers; real compiler workloads probably
  closer to 2-3× now. Acceptable but worth measuring early.
- **Single-maintainer risk.** Justin Ethier maintains Cyclone. Bus factor
  of 1. Mitigated by: it's open source, well-documented, and the codebase
  is comprehensible enough that we could maintain a fork if needed.

## Out of scope

- Performance optimization beyond "works correctly and is fast enough for
  a watch loop." Premature.
- Replacing Cyclone with a different Scheme later. The architecture should
  stay portable enough (target = generic `scheme`, default runtime =
  Cyclone) that swapping is mechanical, but we're not designing for it.
- Erlang-style actor concurrency via Termite. Native threads cover the
  use case; actor model would be additional design surface.
