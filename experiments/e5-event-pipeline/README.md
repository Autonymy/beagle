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
| E5e | Behavioral scoring (40 per-bug tests) | Eliminates line-diff bias; clojure jumps to 90% |
| E5f | Smaller model (Sonnet/Haiku) | Type checker value grows as model capability degrades |

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

### E5e: Full behavioral scoring (both tracks)

| Metric | Beagle (line) | Clojure (line) | Beagle (behavioral) | Clojure (behavioral) |
|--------|:---:|:---:|:---:|:---:|
| Mean accuracy | 66.0% | 70.3% | **85.1%** | **90.0%** |
| Std deviation | 1.7% | 2.1% | 1.5% | 0.0% |
| Checker errors | 0 | n/a | 0 | n/a |

**Key findings:**
- Both tracks improve dramatically under behavioral scoring (+19pp beagle, +20pp clojure)
- The gap narrows slightly (4.3pp line → 4.9pp behavioral) but persists
- Beagle's remaining failures are "over-strict fixes" (type-valid but semantically narrow)
- 0 type-checker errors across all 6 beagle runs (verification guarantee confirmed)
- BUG-09 (missing match case): beagle catches 2/3 runs, clojure catches 0/3

See `results.md` for full analysis and per-bug breakdown.

## Running

```bash
# Build golden beagle reference
bin/beagle-build-all golden/beagle/

# Verify golden reference
clj verify/master.verify.clj

# Run experiment (manual — sets up dir, you launch agent)
bin/run-experiment e5a beagle 1

# E5f: Automated smaller-model trial (setup + agent + score)
bin/run-model-trial sonnet beagle 1
bin/run-model-trial haiku clojure 2

# E5f: Run all 12 trials
bin/run-e5f-all

# E5f: Run only one model tier
bin/run-e5f-all sonnet
```

## Beagle version

Built against: v0.3.0 (commit b4c4427, with/defenum/exhaustive-match)
