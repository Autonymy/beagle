# 009 — E8: repair obligations — the partial oracle backfire

**Date:** 2026-05-17 afternoon  
**Experiment:** E8 (13 modules, 8500 LOC, 35 bugs, 291 partial oracle)

## Setup

E8 reused E4's 13-module system, rewritten with defscalar nominal types.
35 injected bugs (12 scalar confusion + 5 arity + 3 multi-candidate + 15 logic).
Introduced `beagle-fix`: auto-applies 6 high-confidence single-suggestion fixes.

Deliberately used a **partial oracle** (291 of 484 assertions) — hypothesis
was that fewer test signals would disadvantage clojure more than beagle,
since beagle has the checker as a second signal source.

## Result (run 1)

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Assertions | 291/291 | 291/291 |
| Turns | 76 | 36 |
| Wall time | 442s | 215s |
| Bugs fixed | 29 | 10 |

**Beagle took 2x longer despite fixing 3x more bugs.**

## Why

The partial oracle created an asymmetric game:

- **Clojure**: compile immediately → run oracle → see 26 failures → fix only
  the ~10 bugs that affect tested assertions → done. Bugs in untested code
  are invisible and ignorable.

- **Beagle**: checker reports 20 type errors → must fix ALL of them before
  compilation succeeds → then run oracle → already passing. The checker
  forced repair of bugs in untested code paths.

## The discovery

> **Repair obligations are a cost, not just a benefit.**

A type system that blocks compilation makes the agent do work proportional
to total bugs, not tested bugs. When the oracle only covers a fraction of
behavior, this is strictly more expensive than the untyped path.

The partial oracle was designed to disadvantage clojure (fewer signals)
but actually advantaged it (fewer bugs need fixing to pass).

## Response

Rerunning with full 484-assertion oracle (E8 run3) to eliminate the
shortcut. When every bug is tested, clojure can't skip — it must find
and fix all 35 manually. That's where precise diagnostics + auto-fix
should dominate.

Longer-term: `beagle-build-all --warn` to make type errors non-blocking.
