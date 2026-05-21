# E22: Cross-target macro verification

## Question

Do proc macro expansions emit correctly to all 6 targets? The macro
operates at the AST level (pre-emit), so the expanded forms should
go through each target's emitter unchanged. But do they?

## Why this matters

If proc macros only work for `beagle/clj`, they're a Clojure feature.
If they work across all targets, they're a language feature. The
typed AST contract means the expansion produces valid AST regardless
of target — this experiment verifies that claim.

## Targets

| Target | `#lang` | Runtime | Verification |
|--------|---------|---------|-------------|
| Clojure | `beagle` | Babashka | `bb out.clj` |
| ClojureScript | `beagle/cljs` | — | compile only |
| JavaScript | `beagle/js` | Node | `node out.js` |
| Nix | `beagle/nix` | nix-instantiate | `nix eval --file out.nix` |
| Python | `beagle/py` | Python 3 | `python3 out.py` |
| Typed Racket | `beagle/rkt` | raco | `raco make out.rkt` (oracle) |

## Task

Write a proc macro `defconst-set` that generates:
- A set of named constants (defn returning a value)
- A lookup function (cond-based dispatch by name string)

This pattern uses only portable forms (defn, cond, str, =) that
should emit to every target.

## Protocol

1. Write the macro + invocation in a base `.bgl` file
2. For each target:
   a. Copy file, change `#lang` to target variant
   b. Compile: `racket FILE.bgl > out.EXT`
   c. Run/verify with target runtime
   d. Record: compiles? runs? correct output?
3. For Typed Racket target: `raco make` validates type promises

## Success criteria

All 6 targets compile. Targets with available runtimes (Clojure, JS,
Python) produce identical output. Typed Racket passes `raco make`
(oracle validation).

## Known risks

- Nix target may not support all forms (`cond` emits `if/then/else` chains)
- SQL target excluded (no procedural forms)
- CLJS has no local runtime for execution verification

## Files

```
experiments/e22-cross-target-macros/
  DESIGN.md                    — this file
  tasks/
    macro-base.bgl             — base Clojure version
    macro-js.bgl               — #lang beagle/js
    macro-nix.bgl              — #lang beagle/nix
    macro-py.bgl               — #lang beagle/py
    macro-rkt.bgl              — #lang beagle/rkt
  results/
    RESULTS.md                 — target matrix + analysis
```
