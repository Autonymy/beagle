# 005 — E3b: first speed advantage (36%)

**Date:** 2026-05-16 morning  
**Commit:** `a82aa3f`  
**Experiment:** E3b (v2-inventory framework, 5 modules, 1651 LOC)

## Setup

5-module inventory system with 12 injected bugs:
- 4 arity errors (both tracks can detect)
- 3 wrong field access (beagle detects, clojure silent)
- 2 wrong type passed (beagle detects, clojure silent)
- 3 logic bugs (neither detects)

444 verification assertions. Both tracks get structural query tools
(beagle-sig/fields/callers for beagle, clj-sig/fields/callers for clojure).

## Result

Beagle completed 36% faster than clojure (wall clock, averaged over runs).

The advantage came from field-access and type-mismatch bugs: beagle's
checker pointed directly at the wrong accessor with a "did you mean?"
suggestion. Clojure agents had to reason from failing assertions back
to the bug location — reading code, hypothesizing, trying fixes.

## Interpretation

At 1651 LOC / 5 modules, the type checker's localization advantage
outweighs its compilation overhead. The speed gap comes from eliminating
reasoning iterations, not from faster individual operations.

Question for next experiment: does this hold at larger scale?
