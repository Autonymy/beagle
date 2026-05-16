# E5: Event-Sourced Order Pipeline

Production-shaped experiment proving beagle's compile-time safety at scale.

## Domain

Event-sourced e-commerce order processing: events → projections → commands →
handlers → queries. 8 modules, ~3000 LOC per track, 20+ record types with
nullable state fields, heavy cross-module contracts.

## Why this domain

1. **Nullable state is structural** — projections accumulate state from events;
   fields are nil until the relevant event arrives (shipped-at, delivered-at, etc.)
2. **Pattern matching is natural** — event dispatch in handlers
3. **Cross-module contracts are dense** — every handler imports events + projections
4. **Schema evolution is realistic** — adding/splitting events cascades everywhere
5. **Field confusion is easy** — similar records with overlapping field names

## Experiments

| ID | Task | Beagle advantage |
|----|------|-----------------|
| E5a | Fresh build from spec | Compile-time catches mistakes during development |
| E5b | Schema evolution (split OrderPlaced → OrderPlaced + OrderPriced) | Compiler finds all affected call sites |
| E5c | Bug detection (40 injected bugs) | 25 caught at compile time; verified repair loop |

## Module DAG

```
events (leaf)
├── projections (requires events)
├── commands (requires events, projections)
├── handlers (requires events, projections, commands)
├── queries (requires projections)
├── pipeline (requires events, projections, handlers)
├── notifications (requires events, projections)
└── analytics (requires events, projections, queries)
```

## Results (3 runs per track, unlabeled bugs)

### E5c (positional constructors) → E5d (`with` form)

| Metric | Beagle (E5c) | Beagle (E5d) | Clojure (E5c) | Clojure (E5d) |
|--------|:---:|:---:|:---:|:---:|
| Mean accuracy | 63.7% | 66.0% | 68.3% | 70.3% |
| Std deviation | 1.5% | 1.7% | 5.5% | 2.1% |
| Checker errors | 0 | 0 | n/a | n/a |

The `with` form eliminated the bug-surface asymmetry and improved beagle's
score by +2.3pp, but clojure still wins on raw line-level accuracy. The gap
narrowed from -4.6pp to -4.3pp.

Beagle's advantage: **verification** (0 proven checker errors, all runs) and
**consistency** (1.7% std dev). The remaining gap comes from "correct but
different" fixes — agents make type-valid repairs that don't match golden intent.

See `results.md` for full analysis, per-module breakdown, and confounding factors.

## Running

```bash
# Build golden beagle reference
bin/beagle-build-all golden/beagle/

# Verify golden reference
clj verify/master.verify.clj

# Run experiment
bin/run-experiment e5a beagle 1
```

## Beagle version

Built against: v0.3.0 (commit b4c4427, with/defenum/exhaustive-match)
