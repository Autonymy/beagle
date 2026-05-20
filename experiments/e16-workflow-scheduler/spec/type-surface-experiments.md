# E16-T: Type Surface Experiments

## Question

Which type features actually help agents fix bugs, and which are
just tax?

E5 showed Beagle losing to untyped Clojure by 4-7 points because
the checker blocked compilation. The effect system (check/rescue,
deferror, :raises) may make this worse. We need data.

## Setup

### Checker Profiles

```
P0  parse-only       No type checking. Syntax errors only.
P1  basic types      Function sigs, record fields, arity, unknown fields.
P2  structural       + defunion, exhaustive match, defscalar, nil narrowing.
P3  current          + deferror, check/rescue, :raises (unenforced).
```

Each is a strict superset. Implement via `--profile 0|1|2|3` flag
in beagle-check.

### Diagnostic Mode

`beagle-check --agent` — errors only, no lint spam. Separate from
profiles. Implement first because it's useful regardless.

### Bugs

10 bugs. Diverse enough to cover the type system's range. Each is a
single atomic change to the golden scheduler.

| ID | Category | Description | Checker catches? |
|----|----------|-------------|-----------------|
| 1 | off-by-one | `<` vs `<=` in window overlap | No |
| 2 | off-by-one | retry count fence error | No |
| 3 | type confusion | priority sort reversed (ASC↔DESC) | No |
| 4 | type confusion | task-id vs worker-id swap | P2 (defscalar) |
| 5 | missing case | empty task list crashes | No |
| 6 | missing case | match arm missing for DeadlineMissed | P2 (exhaustive) |
| 7 | logic | dependency edges reversed | No |
| 8 | logic | tie-breaking order wrong | No |
| 9 | error handling | error swallowed, returns Ok | No |
| 10 | data flow | reads stale assignment list | No |

8 of 10 bugs are invisible to the checker. This is intentional — the
interesting question is whether types help the agent *reason* even
when the checker doesn't directly catch the bug.

### Agent

Claude Code (Max subscription). Each run is a fresh session:
- Read the buggy code
- Tools: beagle-check, oracle, read/edit files
- Goal: fix the bug, pass the oracle
- Hard cap: 20 turns

Same system prompt across all profiles. Agent doesn't know which
profile it's on.

### Metrics

Per run, record:
- **Fixed?** — oracle passes after agent's edits
- **Wrong fix?** — oracle assertions that previously passed now fail
- **Turns** — conversation turns to fix (or 20 if capped)
- **Check calls** — how many times agent ran beagle-check
- **First check useful?** — did the first beagle-check output
  contain information relevant to the actual bug

That's it. No token counting, no cost estimation, no trajectory
logging. Keep it simple, look at the data.

## Experiments

### T1: Profile Comparison

10 bugs × 4 profiles × 2 reps = **80 runs**

~7 hours at ~5 min/run. Parallelizable.

Look at:
- Fix rate per profile (does P2 beat P0?)
- Wrong-fix rate per profile (does P2 have fewer regressions?)
- P2 vs P3 (does the effect system matter at all?)
- Per-bug breakdown (which bugs does each profile help with?)

If P2 and P3 are basically the same: quarantine the effect system.
If P2 clearly beats P0/P1: structural types are the sweet spot.
If P0 beats everything: types are all tax, rethink everything.

### T2: Diagnostic Noise

10 bugs × 2 modes × 2 reps = **40 runs**

Profile fixed at P2. Compare default output (lint + errors) vs
`--agent` mode (errors only).

If `--agent` is clearly better: ship it as default for agent use.
If no difference: lint noise doesn't matter, move on.

### T3: Annotation Density (Only If T1 Shows P2 Wins)

10 bugs × 3 variants × 2 reps = **60 runs**

Same logic, three annotation levels:
- Minimal: function sigs only, no defscalar
- Standard: current golden code
- Maximum: every binding annotated, defscalar everywhere

If Standard wins: current practice is right.
If Minimal wins: we're over-annotating.
If Maximum wins: we should annotate more.

## Implementation

### Step 1: --agent flag (1 hour)

Filter lint from beagle-check output. Show only errors + summary line.

### Step 2: --profile flag (half day)

Gate check passes behind profile level in check.rkt.

### Step 3: Bug injection (half day)

Write the 10 bug diffs. Compute assertion fingerprints (which oracle
tests each bug breaks). Store in `bugs/` directory.

### Step 4: Runner script (half day)

`bin/run-type-experiment`:
- Takes profile, bug ID, run number
- Copies golden code, applies bug diff
- Launches Claude Code with standardized prompt
- Runs oracle before/after
- Writes result JSON to `results/`

### Step 5: Analysis script (1 hour)

`bin/analyze-type-experiments`:
- Reads result JSONs
- Prints fix rate / wrong-fix rate / avg turns per profile
- Prints per-bug breakdown
- Highlights interesting patterns

No statistical tests in the script. Look at the numbers. If P2 fixes
8/10 bugs and P0 fixes 4/10, you don't need a p-value.

## Timeline

```
Day 1: Implement --agent and --profile flags
Day 2: Write bugs, fingerprints, runner script
Day 3: Run T2 (40 runs, ~3.5 hours)
Day 4: Run T1 (80 runs, ~7 hours)
Day 5: Analyze, write conclusions, decide what to keep/cut
```

## Decision Framework

After T1, fill in this table:

```
Feature              | Helps fix? | Prevents wrong fixes? | Worth it?
---------------------|------------|-----------------------|----------
Basic signatures     |            |                       |
Record field types   |            |                       |
Exhaustive match     |            |                       |
defscalar            |            |                       |
Flow narrowing       |            |                       |
deferror             |            |                       |
check/rescue         |            |                       |
:raises              |            |                       |
```

Features that help: keep in blessed core.
Features that don't help: quarantine or cut.
Features that hurt: cut immediately.
