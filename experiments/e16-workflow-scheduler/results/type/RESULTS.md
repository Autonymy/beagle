# E16-T Type Surface Experiment Results

**Date:** 2026-05-21
**Researcher:** Tom Passarelli
**Model:** Claude Sonnet (via Claude Code `--print`)
**Codebase:** Beagle scheduler (6 files, ~800 LOC, 125 oracle tests)

## Research Question

Does a type system help LLM agents work on real software?

Two sub-questions, tested with separate experiments:

1. **T1 (Bug Fixing):** Does the type checker profile level affect how
   fast an agent fixes single-line bugs when given a comprehensive test
   suite?

2. **F1 (Feature Building):** Do types help agents build features
   correctly and completely when test coverage is partial?

## T1: Bug Fixing Under Type Profiles

### Design

- 3 representative bugs × 4 type checker profiles × 1 rep = 12 trials
- Bugs: `01-window-overlap` (logic), `04-id-swap` (type-adjacent),
  `07-edges-reversed` (graph)
- Profiles: P0 (parse only), P1 (basic types), P2 (structural),
  P3 (full with effects)
- All 3 bugs are checker-invisible across all profiles (the type checker
  does not catch them — only oracle tests reveal the bug)
- Agent gets: buggy code, type checker, full oracle (125 tests)
- Randomized trial order, opaque workspace IDs

### Results

| Bug \ Profile        | P0     | P1      | P2     | P3     |
|----------------------|--------|---------|--------|--------|
| 01-window-overlap    | 88s    | 261s    | 77s    | 70s    |
| 04-id-swap           | 76s    | 334s    | 70s    | 70s    |
| 07-edges-reversed    | 72s    | 212s    | 97s    | 98s    |
| **Average**          | **79s**| **269s**| **81s**| **79s**|

- **12/12 clean fixes.** Every profile succeeded on every bug.
- **Zero type-checker calls** across all 12 trials. The agent went
  straight to oracle tests every time.
- P0, P2, P3 are statistically indistinguishable (~80s average).
- **P1 is 3.4× slower.** Its 2 false-positive type errors (from missing
  flow narrowing) actively distracted the agent.

### T1 Conclusions

When an agent has a comprehensive test suite, the type checker is
irrelevant for single-bug repair. The agent ignores it entirely.

A half-baked type checker with false positives (P1) is actively worse
than no type checker at all — the agent wastes time investigating
phantom errors.

This result is correct but limited: it only measures "types vs tests"
in a setup where tests provide a complete signal. Real software doesn't
have a perfect oracle.

## F1: Feature Building Under Type Profiles

### Design

- 2 features × 2 profiles (P0, P2) × 1 rep = 4 trials
- Profiles: P0 (no types) vs P2 (structural types with exhaustive match)
- Agent gets: golden source code, feature spec, partial visible tests
- **Two oracles:**
  - *Visible oracle:* tests the agent can run during development
  - *Hidden oracle:* post-agent measurement of structural completeness
    (match site updates, error handling, validator coverage, regressions)
- The hidden oracle measures whether types help the agent discover
  obligations that tests don't cover.

### Feature A: Task Groups

Add optional group membership to tasks. If any task in a group fails,
all remaining unscheduled tasks in the same group also fail with a
`GroupFailure` reason.

**Requires:**
- New `group` field on `Task` record
- New `GroupFailure` variant in `FailureReason` union
- Update 3 exhaustive match sites in `errors.bgl`
- Implement group failure propagation in scheduler accumulator
- Update `make-task` and `make-simple-task` constructors

| Metric          | P0 (no types) | P2 (structural) |
|-----------------|---------------|-----------------|
| Visible tests   | 2/5           | **5/5**         |
| Hidden tests    | 7/11+         | **11/11+**      |
| Duration        | 600s (timeout)| 600s (timeout)  |

P2 passed all visible tests and nearly all hidden structural
obligations. P0 correctly added the types and updated match sites in
`errors.bgl` but failed to implement group failure propagation logic
correctly — the `GroupFailure` reason was never actually produced by
the scheduler.

Both agents timed out (10 min limit), but P2 arrived at a substantially
more correct implementation.

### Feature B: Resource Maintenance Windows

Add maintenance windows to resources (analogous to worker unavailability).
Tasks requiring a resource cannot be scheduled during its maintenance
window.

**Requires:**
- New `maintenance-windows` field on `Resource` record
- New `ResourceMaintenance` variant in `FailureReason` union
- New `ResourceInMaintenance` variant in `ViolationKind` union
- Update matcher to check resource maintenance windows
- Update validator to detect maintenance violations
- Update all match sites in `errors.bgl` and `validator.bgl`
- Touches 5 of 6 source files

| Metric          | P0 (no types) | P2 (structural) |
|-----------------|---------------|-----------------|
| Visible tests   | **5/5**       | **5/5**         |
| Hidden tests    | **12/12**     | **12/12**       |
| Duration        | **274s**      | 372s            |

Both profiles aced every test — visible and hidden. P0 was faster.

### F1 Conclusions

**Types helped on the harder feature but not the easier one.**

Feature A required the agent to:
1. Understand how failure propagation flows through the scheduling loop
2. Thread a new `failed-groups` accumulator through the reduce
3. Coordinate group state with existing dependency-failure logic
4. Update exhaustive match sites it was never tested on

This is where P2's exhaustive match checking provided a reasoning
scaffold — the type checker flagged every incomplete match, and the
structural constraints guided the agent toward a correct implementation.

Feature B was structurally harder (5 files, 2 union types) but
conceptually simpler — each change was local and mechanical. The agent
at P0 had no trouble finding all the sites via code reading.

**The differentiator is reasoning complexity, not codebase size.**
Types help when the task requires coordinating structural changes across
shared state. They don't help when the task is "add a field and check it
everywhere" — that's just grep.

## Synthesis

| Finding | Evidence |
|---------|----------|
| Types don't help agents fix bugs | T1: P0 = P2 = P3, zero checker calls |
| False-positive type errors actively hurt | T1: P1 is 3.4× slower |
| Types help build complex features | F1-A: P2 passes 5/5 visible vs P0's 2/5 |
| Types don't help build mechanical features | F1-B: P0 = P2 |
| The value of types is coordination, not detection | Agent never uses checker for bug finding; checker helps when threading new state through existing patterns |

**The sharp finding:**

> Types are not a bug-finding tool for agents. They are a reasoning
> scaffold that helps when the task requires coordinating structural
> changes across shared state — exactly the kind of work that happens
> when requirements evolve, APIs change, and features get built.
>
> The value isn't "catching mistakes." It's "constraining the solution
> space so the agent makes fewer mistakes in the first place."

## Caveats

- **N=1 per cell.** These are pilot results. Run 3-5 reps on Feature A
  (where the P0/P2 gap was largest) to confirm the signal.
- **Small codebase.** 6 files means the agent can read everything. In a
  larger codebase, the navigation advantage of type errors would likely
  increase.
- **Single model.** All trials used Claude Sonnet. A weaker model might
  benefit more from type guidance.
- **Beagle-specific.** The type checker profiles are specific to Beagle's
  implementation. Results may not generalize to other type systems.
- **Feature selection.** Two features is enough for a pilot, not a claim.
  More features with varying complexity would strengthen the signal.

## Experiment Infrastructure

- `bin/run-T1` — T1 batch runner (3 bugs × 4 profiles)
- `bin/run-F1` — F1 batch runner (2 features × 2 profiles)
- `bin/run-type-experiment` — Single T1 trial runner
- `bin/run-feature-experiment` — Single F1 trial runner
- `bin/fingerprint-type-bugs` — Bug/profile visibility matrix
- `type-bugs/` — 10 bug injection scripts
- `feature-tasks/` — 2 feature specs with visible/hidden oracles
