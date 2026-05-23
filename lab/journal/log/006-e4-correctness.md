# 006 — E4: first correctness divergence at 8.5K LOC

**Date:** 2026-05-16 evening  
**Commit:** `7d790e4`  
**Experiment:** E4 (13 modules, 8570 LOC, 484 assertions, 35 bugs)

## Setup

Scaled the v2 framework to 13 modules with a full dependency DAG
(5 layers deep). 35 injected bugs. Full oracle (484 assertions).
Introduced batch tools: `beagle-check-all` (10x) and `beagle-build-all`
(9x vs sequential).

## Result

**First correctness divergence:**
- Beagle: 3/3 runs pass all 484 assertions
- Clojure: 0/3 runs pass all assertions

Clojure agents got stuck in reasoning loops on cross-module bugs where
the runtime error (Java stack trace) didn't point at the actual cause.
They'd fix the symptom, break something else, iterate, and eventually
time out or declare done with failures remaining.

## Interpretation

At 8.5K LOC, the reasoning cost of diagnosing bugs from runtime behavior
alone exceeds the agent's effective budget. The type checker's precise
localization ("line 42: expected CarrierId, got ShipmentId") keeps the
repair loop bounded regardless of codebase size.

This is the first hard evidence that beagle produces qualitatively
different outcomes, not just faster ones.
