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

## E5c Results (3 runs per track, unlabeled bugs)

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Mean accuracy | 63.7% | 68.3% |
| Std deviation | 1.5% | 5.5% |
| Mean time | 319s | 226s |
| Checker errors | 0 (all runs) | n/a |

The type checker catches 25/40 bugs at compile time and verifies fixes with
certainty. But on raw line-level accuracy, the clojure agent scores higher —
Claude Opus 4 is good enough at code reading to find most bugs by inspection.

Beagle's advantage is **verification** (0 proven checker errors) and
**consistency** (1.5% std dev vs 5.5%). Clojure's advantage is **speed** and
**higher peak accuracy**.

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

Built against: v0.2.0 (commit f91b70a)
