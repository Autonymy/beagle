# 010 — Direction: repair compiler, not type system

**Date:** 2026-05-17 afternoon

## The dead end

Beagle was converging on "typed Clojure for LLMs." This hits a ceiling:

- Conventional type systems prove conservative fragments
- They create repair obligations that can make agents slower
- At Opus-level intelligence, agents can reason through many bugs
  that types would catch — the marginal value of proof decreases
- The checker blocks compilation, creating a "fix everything first"
  tax that untyped languages don't pay

## The reframe

> Beagle does not maximize type purity. Beagle minimizes repair distance.

Repair distance = the work an agent must do between "here is a bug"
and "here is the fix applied." Types reduce repair distance for shape
errors. But only if they don't simultaneously increase it by blocking
execution.

## The position

```
PL people optimize for soundness.
Dynamic people optimize for flexibility.
AI tooling people assume the model can reason through anything.

Beagle optimizes for cheap, local, machine-actionable repair.
```

The compiler's job is not to reject programs. It is to:
1. Diagnose what's wrong (precisely, structurally)
2. Rank findings by confidence
3. Produce executable repair plans (diffs, not prose)
4. Emit code regardless — annotate, don't block

## The tagline

> **Beagle: the language where the compiler does the debugging.**

## Concrete next steps

1. `beagle-build-all --warn` — emit despite type errors when codegen
   is still possible. Diagnostics become annotations, not blockers.
   Parse errors and unresolvable bindings remain fatal.

2. Confidence tiers everywhere — AUTO-FIXED / SUGGESTED / DIAGNOSTIC
   already exist in beagle-fix. Extend to all checker diagnostics.

3. Machine-readable repair plans — not just "did you mean X?" but
   `{"replace": "order-id", "with": "order-amount", "span": [37,5,37,13]}`.

4. Keep hard checks boring — defscalar, arity, field existence,
   accessors, nullable, exhaustive match. These work. Don't over-engineer.

5. Soft checks deferred — name/operation mismatch, arithmetic direction
   analysis are interesting but premature. Ship --warn first.

## The test

If `--warn` mode makes beagle strictly faster than clojure on E8
(same diagnostics, no compilation tax), the thesis is validated:
repair-oriented compilation beats both conventional types and no types.
