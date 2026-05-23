# 007 — E5: the type tax — beagle loses on behavioral scoring

**Date:** 2026-05-16 evening  
**Commits:** `bc791a7`–`d5fe418`  
**Experiment:** E5 (event-sourced pipeline, 8 modules, 5K LOC, 40 bugs)

## Setup

Event-sourced pipeline with projections using `with` (typed record update).
40 injected bugs. Behavioral scoring: per-bug test suites, not just
pass/fail on the full oracle.

## Results (E5c–E5e)

| Metric | Beagle | Clojure |
|--------|--------|---------|
| E5c line-diff | 63.7% (sd 1.5%) | 68.3% (sd 5.5%) |
| E5d with-form | 66.0% | 70.3% |
| E5e behavioral | — | 90% (jumped with per-bug tests) |

Beagle consistently lost by 4–7 percentage points on behavioral scoring.
Clojure agents were faster at "try it and see" iteration because they
could compile and run immediately. Beagle agents spent time satisfying
the checker on bugs that didn't affect the behavioral tests.

## Discovery: the type tax

> A type system that blocks compilation forces the agent to fix everything
> before it can verify anything. When not all bugs are tested, this is
> strictly more work than the untyped path.

The checker's value (precise diagnostics) was being offset by its cost
(mandatory full repair before compilation). The agent was doing correct
work — just more of it than needed to pass the oracle.

## Implication

Beagle needs a way to emit code despite type errors. The diagnostics
should inform repair priority, not block execution.
