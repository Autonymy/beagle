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
| E5c | Bug detection (40 injected bugs) | 24 caught at compile time vs 0 for Clojure |

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

## E5c Results

Beagle fixed 27/40 bugs with 0 wrong fixes. Clojure fixed 0/40 and introduced
5 wrong fixes.

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Correctly fixed | 27/40 | 0/40 |
| Partially fixed | 8/40 | 0/40 |
| Wrong fixes introduced | 0 | 5 |
| Compile-time bugs caught | 13 | 0 |
| Post-fix verification | 0 checker errors | no verification path |
| Wall time | 293s | 235s |
| Tool calls | 69 | 57 |

**Scoring rubric:** "Correct" = matches the intended golden repair, or is
behaviorally equivalent where equivalence is clear. "Partial" = the bug was
addressed but the fix diverges from golden in ways that may introduce edge-case
differences. "Wrong fix" = the edit changes behavior in a direction that does
not resolve the bug or introduces a new defect.

### The cascade

The type checker directly exposed 13 bugs, but the agent fixed 14 more outside
the checker's direct reach — missing match cases, logic errors, nil-access
patterns that the type system does not cover. The checker did not merely catch
errors; it reduced uncertainty enough for the agent to reason productively about
the rest of the system.

`beagle-check-all` verifies that the fixed codebase satisfies beagle's
cross-module type/contracts layer. It does not prove full semantic correctness —
but it eliminates entire categories of defect (wrong field, wrong arity, type
mismatch) with certainty, giving the agent a verified foothold from which to
inspect the remainder.

The clojure agent, facing the same 40-bug search space with no reliable signal,
could not build an accurate enough mental model of 3000 LOC to fix even a single
bug correctly. Several of its attempts made things worse.

### The thesis

Beagle turns AI coding from speculative editing into checked repair.

The difference was not model intelligence — both agents used the same model. It
was feedback quality. Beagle gave the agent exact, cross-module diagnostics and a
verification loop. Raw Clojure forced the agent to inspect 3000 LOC with no
reliable signal.

The broader claim: AI agents do better when the language gives them small, typed,
local, checkable facts. This is not a claim about syntax preferences. It is a
claim about the feedback loops that make AI-assisted programming reliable.

### Limitations and next steps

This is a single trial. To make this result hold up to scrutiny:

1. Multiple trials (3–5 per track) to establish variance
2. Publish exact prompts and raw agent transcripts
3. Define and publish the bug injection methodology
4. Add a third baseline: Clojure + spec/Malli/core.typed (to separate
   "types exist" from "beagle-specific types")
5. Larger injected bug counts and adversarial bug placement

See `results.md` for the full bug-by-bug breakdown.

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
