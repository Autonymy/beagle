# E10: Workflow Compression

**Hypothesis:** Giving the agent a machine-applicable patch (`--emit-patch`)
instead of a human-readable repair queue reduces wall time and turns, because
the agent spends zero reasoning tokens on mechanical fixes.

**Baseline:** E9 (same system, same bugs, Opus 4.6).

**Change:** The beagle spec tells the agent to run `beagle-repair --emit-patch`
first and `git apply` the result before doing anything else. The Clojure spec
is unchanged from E9.

## Setup

Same E8 system: 13 modules, ~8500 LOC, 35 injected bugs, 484 assertions.
Buggy source copied from `../e8-scaled/buggy/`.

## Protocol

1. Copy buggy code into `trials/e10-beagle-run{1,2,3}/` and `trials/e10-clojure-run{1,2,3}/`
2. Run Claude Code with `--dangerously-skip-permissions` and the appropriate spec
3. Record: turns, wall time, output tokens, pass rate
4. Compare against E9 averages

## Expected results

- Beagle mechanical bugs (accessor swap, arg swap, operand swap, value swap)
  resolve in ~1 turn instead of ~5-10 turns each
- Semantic bugs (logic errors) take the same number of turns
- Net: wall time gap widens from -29% (E9) toward -40-50%
- Correctness: still 3/3 both tracks

## Metrics

| Metric | E9 Beagle | E9 Clojure | E10 Beagle (predicted) |
|--------|-----------|------------|------------------------|
| Turns | 77 | 88 | ~50-60 |
| Wall time | 421s | 595s | ~280-350s |
| Tokens | 21,603 | 33,944 | ~14,000-18,000 |

## Run commands

```bash
# Beagle track
cd trials/e10-beagle-run1
claude --dangerously-skip-permissions -p "$(cat ../../spec/e10-beagle.md)"

# Clojure track
cd trials/e10-clojure-run1
claude --dangerously-skip-permissions -p "$(cat ../../spec/e10-clojure.md)"
```
