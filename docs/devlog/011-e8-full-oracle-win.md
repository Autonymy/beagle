# 011 — E8 run3: beagle wins with full oracle

**Date:** 2026-05-17 afternoon  
**Experiment:** E8 run3 (13 modules, 8500 LOC, 35 bugs, 484 full oracle)

## Setup

Same E8 system as run1, but with the full 484-assertion oracle instead
of the partial 291. This eliminates clojure's ability to shortcut by
only fixing oracle-visible bugs.

## Results

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Assertions | 484/484 | 484/484 |
| Turns | 76 | 92 |
| Wall time | 375s | 485s |
| Output tokens | 20,089 | 25,479 |

**Beagle: 17% fewer turns, 23% faster, 21% fewer tokens.**

## Comparison with run1 (partial oracle)

| | Run1 (partial) | Run3 (full) |
|--|---|---|
| Beagle | 76 turns, 442s | 76 turns, 375s |
| Clojure | 36 turns, 215s | 92 turns, 485s |
| Winner | Clojure (2x faster) | Beagle (23% faster) |

Beagle's performance was nearly identical across both runs — the checker
drives a fixed workflow regardless of oracle size. Clojure's performance
degraded dramatically when it couldn't shortcut (36→92 turns, 215→485s).

## Interpretation

When every bug is tested, clojure must find and fix all 35 manually:
read code, hypothesize, edit, rerun, repeat. Beagle's workflow:
`beagle-fix` auto-applies 6, checker diagnoses 14 more with precise
locations and suggestions, then behavioral iteration for the 15 logic bugs.

The advantage comes from the first ~20 bugs being nearly mechanical
for beagle (apply fix, move on) vs. requiring multi-step reasoning
for clojure (read stack trace or failing assertion, locate cause, fix).

## The asymmetry explained

- Beagle's cost is ~constant regardless of oracle coverage (checker
  forces full repair either way)
- Clojure's cost is proportional to oracle coverage (more assertions =
  more bugs surfaced = more manual reasoning needed)

At full coverage, beagle's "repair obligation tax" becomes a feature:
it front-loads work that clojure defers, but does it cheaply via
structured diagnostics rather than expensive agent reasoning.

## Follow-up: --warn mode (non-blocking type errors)

Implemented `beagle-build-all --warn` and ran same experiment.

| Variant | Turns | Duration |
|---------|-------|----------|
| Beagle --warn | 74 | 385s |
| Beagle blocking | 76 | 375s |
| Clojure | 92 | 485s |

Result: essentially tied with blocking mode. The agent fixed all type
warnings before verifying regardless of whether compilation was gated.
At Opus intelligence, the agent recognizes type warnings as high-value
signals and acts on them eagerly — blocking vs non-blocking is irrelevant.

The value of `--warn` is safety (prevents unfixable type errors from
blocking all progress), not speed. The actual speedup comes from the
diagnostics themselves, not from when they're enforced.
