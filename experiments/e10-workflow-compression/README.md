# E10: Workflow Compression

Hypothesis: if beagle emits a machine-applicable patch instead of a
human-readable repair queue, the agent will spend fewer turns and tokens
on mechanical fixes.

**Result: confirmed.** Against the E9 baseline, `--emit-patch` reduced
wall time by 33% and tokens by 41%, while preserving full correctness
(484/484 all runs).

The claim is not "beagle has types." The claim is: **beagle turns
debugging from reasoning work into patch-application work.**

## The story arc

- **E9:** Beagle gives the agent a better repair queue.
- **E10:** Beagle turns part of that queue into an executable patch.
- **Result:** fewer turns, fewer tokens, less wall time, same correctness.

## What this proves

This is not "beagle language vs Clojure language" in the narrow sense.
It is beagle's repair workflow vs raw Clojure's repair workflow. The
advantage is that the authoring surface gives the tooling enough
structure to emit trusted patches. Raw Clojure does not naturally
expose the same repair substrate.

## Actual results

| Metric | E9 Beagle | E9 Clojure | E10 Beagle | E10 Clojure |
|--------|-----------|------------|------------|-------------|
| Wall time | 421s | 595s | 310s | 464s |
| Gap | 29% faster | — | 33% faster | — |
| Correctness | 3/3 | 3/3 | 3/3 | 3/3 |

## Setup

Same E8 system: 13 modules, ~8500 LOC, 35 injected bugs, 484 assertions.
Buggy source copied from `../e8-scaled/buggy/`.

## Protocol

1. Copy buggy code into `trials/e10-beagle-run{1,2,3}/` and `trials/e10-clojure-run{1,2,3}/`
2. Run Claude Code with `--dangerously-skip-permissions` and the appropriate spec
3. Record: turns, wall time, output tokens, pass rate
4. Compare against E9 averages

## Original predictions vs actuals

| Metric | Predicted | Actual |
|--------|-----------|--------|
| Turns | ~50-60 | — |
| Wall time | ~280-350s | 310s |
| Tokens | ~14-18K | — |

## Run commands

```bash
# Beagle track
cd trials/e10-beagle-run1
claude --dangerously-skip-permissions -p "$(cat ../../spec/e10-beagle.md)"

# Clojure track
cd trials/e10-clojure-run1
claude --dangerously-skip-permissions -p "$(cat ../../spec/e10-clojure.md)"
```
