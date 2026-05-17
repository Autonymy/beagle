# Beagle Experiment Report

## What is Beagle?

Beagle is an agent-native authoring layer for Clojure. It does not try
to make the type system maximally pure. It tries to make bugs maximally
repairable. The compiler emits structured diagnostics, repair traces,
and executable patches so LLM agents spend less time reasoning through
mechanical fixes.

**Thesis:** Mechanical bugs should not require cognition. They should
compile into patches.

## Summary

Across twelve experiments, two findings held:

1. **Static type checking helps ~25% regardless of language.** Python +
   mypy, Beagle + checker, Clojure + kondo — all show similar gains
   over their untyped baselines.
2. **Python + mypy is the fastest and most correct track.** Fastest for
   bug repair (255s avg), and 3/3 correct without a test oracle. Beagle
   matches per-bug speed at the median but has higher variance.

The E4 correctness divergence (Beagle 3/3 vs Clojure 0/3) is a static
typing result, not a Beagle result — Python + mypy also achieves 3/3
without an oracle. Clojure fails because it lacks static checking, not
because it lacks Beagle.

Within the Clojure ecosystem, Beagle is a strict improvement (328s vs
365s best Clojure, with 4x more static errors caught). Against Python,
Beagle has no measurable advantage on any tested axis.

## Experiment design

All experiments use the same domain: an inventory & order management
system with typed records, cross-module dependencies, and a behavioral
verification oracle (assertions that define correct behavior).

An LLM agent (Claude Code, Opus 4.6 unless noted) receives buggy source
code and must fix all bugs until the oracle passes. We measure wall time,
turns, output tokens, and correctness (pass rate).

Three language tracks:
- **Beagle** — typed, with checker + repair toolchain (agent-native repair compiler)
- **Clojure** — untyped, same runtime, structural query tools only
- **Python** — typed (`@dataclass` + type annotations), mypy available

| System | Modules | LOC (beagle / clj / py) | Bugs | Assertions |
|--------|---------|------------------------|------|------------|
| v2-inventory | 5 | 1651 / 1235 / — | 12 | 444 |
| e8-scaled | 13 | 8500 / 4700 / 7200 | 35 (clj/bgl) / 30 (py) | 484 |

## Results at a glance

| Experiment | Beagle | Clojure | Python | Key finding |
|-----------|--------|---------|--------|-------------|
| E3b (1.2K, no oracle) | 219s, 3/3 | 344s, 3/3 | — | Beagle 36% faster |
| E4 (8.5K, no oracle) | 449s, **3/3** | 439s, **0/3** | — | First correctness divergence |
| E8 (8.5K, full oracle) | 375s, 3/3 | 485s, 3/3 | — | Beagle 23% faster |
| E9 (+ repair toolchain) | 421s, 3/3 | 595s, 3/3 | — | Beagle 29% faster, 36% fewer tokens |
| E10 (+ emit-patch) | 310s, 3/3 | 464s, 3/3 | — | Beagle 33% faster |
| E11 Opus | 310s | 464s | — | 33% beagle advantage |
| E11 Sonnet | 395s | 411s | — | 4% beagle advantage |
| E11 Haiku | 276s | 281s | — | 2% beagle advantage |
| E8 Python | — | — | 346s, 3/3 | Faster than clj+bgl E9; slower than bgl E10 |
| E12 Python+mypy | — | — | 255s, 3/3 | **Fastest track overall** |
| E12 Beagle+cheatsheet | 328s (median 299s) | — | — | Per-bug parity at median; high variance |
| E12a Clojure+kondo | — | 365s, 3/3 | — | 25% faster than E8 Clojure |
| E4 Python+mypy (no oracle) | — | — | 362s, **3/3** | Matches Beagle E4 correctness |

---

## Detailed results

### E3b — Bug detection without oracle (5 modules, 1.2K LOC, 12 bugs)

No test suite provided. Beagle agents have `beagle-check`; Clojure
agents have only code reading and grep.

| Run | Beagle score | Beagle wall | Clojure score | Clojure wall |
|-----|-------------|-------------|---------------|-------------|
| 1 | 436/444 | 245s | 435/444 | 316s |
| 2 | 435/444 | 175s | 435/444 | 454s |
| 3 | 435/444 | 237s | 435/444 | 262s |
| **Avg** | | **219s** | | **344s** |

Both tracks found all 12 injected bugs every run (8-9 assertion
discrepancies are golden-code spec disagreements). **Beagle 36% faster.**
Beagle uses more tokens but less wall time — the checker provides dense
signal that drives faster iteration.

### E4 — Scaled bug detection without oracle (13 modules, 8.5K LOC, 35 bugs)

Same task at 5x scale. No test oracle. Beagle has checker + 5 query
tools. Clojure has 4 structural query tools (no type info).

| Run | Beagle | Wall | Clojure | Wall |
|-----|--------|------|---------|------|
| 1 | 484/484 | 436s | 484/484* | 307s |
| 2 | 484/484 | 468s | CRASH | 244s |
| 3 | 484/484 | 444s | 484/484* | 570s |
| **Avg** | **3/3 clean** | **449s** | **0/3 clean** | **439s** |

*Clojure runs required manual correction (extra closing paren in
`target-achievement-pct` — all 3 agents made this error independently).
Run 2 also missed a bug causing runtime crash.

**First correctness divergence — the most important result in this
report.** Wall time roughly equal (~2%), but Beagle produces 3/3 clean
runs while Clojure produces 0/3. When the oracle is absent, Clojure can
appear to pass but still produce dirty output requiring manual
correction. Beagle's checker prevents this class of failure entirely. At
scale, the value shifts from speed to correctness.

### E8 — Full oracle baseline (13 modules, 8.5K LOC, 35 bugs, 484 assertions)

Behavioral oracle provided (484 assertions). Single runs.

| Metric | Beagle | Clojure |
|--------|--------|---------|
| Result | 484/484 | 484/484 |
| Turns | 76 | 92 |
| Duration | 375s | 485s |
| Output tokens | 20,089 | 25,479 |

**Beagle 23% faster.** With full oracle coverage, Clojure can't
shortcut. Beagle's auto-fix makes the first 20 type errors mechanical,
leaving only 15 logic bugs for behavioral iteration.

Notable: with a *partial* oracle (291 assertions), Clojure was 2x
faster — it only had to fix bugs that affected tested code. Beagle
was forced to fix all 20 type errors regardless. Beagle's advantage
requires sufficient test coverage.

### E9 — Repair toolchain (3 runs each)

Beagle gets the full repair toolchain (`beagle-repair`, `beagle-trace`,
`beagle-cascade`, `beagle-specfix`, `beagle-blame`). Clojure gets
oracle + structural query tools.

| Run | Beagle turns | wall | tokens | Clojure turns | wall | tokens |
|-----|-------------|------|--------|---------------|------|--------|
| 1 | 83 | 437s | 23,257 | 77 | 534s | 30,486 |
| 2 | 82 | 441s | 22,255 | 90 | 588s | 32,195 |
| 3 | 67 | 386s | 19,297 | 96 | 663s | 39,152 |
| **Avg** | **77** | **421s** | **21,603** | **88** | **595s** | **33,944** |

Correctness: 3/3 both tracks.

| Metric | Delta |
|--------|-------|
| Turns | -12% |
| Wall time | **-29%** |
| Output tokens | **-36%** |

Clojure variance is higher (534–663s vs 386–441s). The repair toolchain
gives Beagle more targeted information per turn, producing less
exploratory output.

### E10 — Workflow compression (`--emit-patch`)

Beagle's `--emit-patch` emits a unified diff that auto-applies 6
mechanical fixes via `git apply` before the agent starts reasoning.

| Run | Beagle | Clojure | Notes |
|-----|--------|---------|-------|
| 1 | ~~659s~~ | 494s | Invalid — 3 `--emit-patch` toolchain bugs |
| 2 | 285s | 404s | |
| 3 | 334s | 494s | |
| **Avg** | **310s** | **464s** | Beagle: valid runs only |

Correctness: 3/3 both tracks.

**Beagle 33% faster.** The 6 patched fixes eliminate 15 of 33 baseline
failures (45%). The remaining 18 are semantic bugs requiring judgment.

| Metric | E9 → E10 beagle | E9 → E10 clojure |
|--------|-----------------|------------------|
| Wall time | 421s → 310s (-26%) | 595s → 464s (-22%) |
| Gap vs clojure | 29% → **33%** | — |

### E11 — Model tier

Same E8 system tested across Opus, Sonnet, and Haiku.

| Config | Wall time | Beagle advantage |
|--------|----------|-----------------|
| Opus + Beagle | 310s | 33% faster |
| Opus + Clojure | 464s | baseline |
| Sonnet + Beagle | 395s | 4% faster |
| Sonnet + Clojure | 411s | |
| Haiku + Beagle | 276s | 2% faster |
| Haiku + Clojure | 281s | |

Correctness: all configurations 484/484.

**Beagle's advantage scales with model capability.** Opus extracts 33%
from the structured repair tools. Sonnet and Haiku fall back to
sequential "read-find-fix" regardless of tooling.

Directionally, Beagle appears to enable model-tier arbitrage: Sonnet +
Beagle (395s) beats Opus + Clojure (464s) at ~5x lower token cost.
Sample sizes are small (2 runs each at Sonnet, 1 at Haiku) — treat as
promising, not settled.

### Python reference track (13 modules, 7.2K LOC, 30 bugs)

Same E8 domain ported to Python with `@dataclass` and type annotations.
30 bugs (vs 35 for Beagle/Clojure — 5 Clojure-specific patterns don't
translate). Agent has mypy + behavioral verify.

| Run | Result | Turns | Wall | Output tokens | Cost |
|-----|--------|-------|------|---------------|------|
| 1 | 484/484 | 54 | 324s | 18,174 | $4.40 |
| 2 | 484/484 | 64 | 353s | 20,624 | $4.86 |
| 3 | 484/484 | 61 | 360s | 19,334 | $4.73 |
| **Avg** | **3/3** | **60** | **346s** | **19,377** | **$4.66** |

**None of the agents used mypy.** All went straight to `python3 verify.py`
and iterated from behavioral feedback.

| Track | Avg wall time | Per-bug time | Bugs |
|-------|-------------|-------------|------|
| Beagle E10 | 310s | 8.9s | 35 |
| **Python** | **346s** | **11.5s** | **30** |
| Beagle E9 | 421s | 12.0s | 35 |
| Clojure E9 | 595s | 17.0s | 35 |

Python beats Clojure by 42% and Beagle E9 by 18% in absolute wall
time. Per-bug, Python (11.5s) is comparable to Beagle E9 (12.0s).
**Beagle E10 still beats Python by 10%** — the repair compiler, not
the type system, is the differentiator.

### E12 — Fair toolchain comparison (best tools, forced workflow)

E8 Python agents never used mypy. E8 Clojure agents had no static
linter. To isolate the effect of static checking across ecosystems, E12
gives each track its best available static tool and forces a
"lint-first" workflow.

**Static tool coverage on the same 35/30 bugs:**

| Tool | Errors caught | Coverage |
|------|--------------|----------|
| `beagle-check` | 20 type errors | 57% of 35 bugs |
| `mypy` | 16 type errors | 53% of 30 bugs |
| `clj-kondo` | 5 arity errors | 14% of 35 bugs |

#### E12 Python + mypy (forced workflow, 3 runs)

Prompt instructs: "Run mypy FIRST. Fix ALL mypy errors before running
verify."

| Run | Turns | Wall | Cost | Result |
|-----|-------|------|------|--------|
| 1 | 70 | 232s | $1.64 | 484/484 |
| 2 | 71 | 170s | $1.46 | 484/484 |
| 3 | 74 | 362s | $2.24 | 484/484 |
| **Avg** | **72** | **255s** | **$1.78** | **3/3** |

**Python + mypy is 26% faster than Python without mypy** (255s vs 346s).
Front-loading static diagnostics helps regardless of language.

#### E12 Beagle + cheatsheet (full language reference in prompt, 3 runs)

Previous Beagle prompts gave minimal language context — the agent had to
learn Beagle's syntax by reading source files. Python agents already
*know* Python from training data. To test whether this asymmetry
explains the gap, this variant prepends the 154-line consumer cheatsheet
to the prompt.

| Run | Turns | Wall | Cost | Result |
|-----|-------|------|------|--------|
| 1 | 84 | 299s | $2.12 | 484/484 |
| 2 | 80 | 413s | $2.96 | 484/484 |
| 3 | 67 | 271s | $2.40 | 484/484 |
| **Avg** | **77** | **328s** | **$2.49** | **3/3** |

**12% faster than E8 Beagle** (328s avg vs 375s). High variance
(271–413s) — the cheatsheet helps but doesn't eliminate training-data
disadvantage. Median (299s) is more representative than the mean.

#### E12 Beagle combined (distilled cheatsheet + emit-patch + daemon + syntax checker, 1 run)

Kitchen sink: shorter "Clojure-delta" cheatsheet, `--emit-patch`
auto-patch, daemon warm-start, paren balance checker.

| Run | Turns | Wall | Cost | Result |
|-----|-------|------|------|--------|
| 1 | 72 | 347s | $1.80 | 484/484 |

**Slower than cheatsheet-only** (347s vs 328s avg). The extra workflow
steps (apply patch, start daemon, run syntax checker) added prompt
complexity that offset their value. The simpler prompt outperforms the
kitchen sink — a useful negative result.

#### E12a Clojure + clj-kondo (forced lint-first workflow, 3 runs)

Prompt instructs: "Run clj-kondo FIRST. Fix ALL errors before running
verify."

| Run | Turns | Wall | Cost | Result |
|-----|-------|------|------|--------|
| 1 | 82 | 489s | $3.86 | 484/484 |
| 2 | 42 | 287s | $3.06 | 484/484 |
| 3 | 35 | 319s | $3.35 | 484/484 |
| **Avg** | **53** | **365s** | **$3.42** | **3/3** |

**25% faster than E8 Clojure** (365s vs 485s). clj-kondo only catches 5
arity errors, yet the structured "lint → fix → verify" workflow itself
improves the agent's approach. High variance (287–489s) reflects
Clojure's lack of static signal — after the 5 arity fixes, it's back
to behavioral-only iteration.

#### E12b Clojure + clj-kondo (naturalistic — tools available, not forced, 3 runs)

| Run | Turns | Wall | Cost | Result |
|-----|-------|------|------|--------|
| 1 | 66 | 445s | $3.94 | 484/484 |
| 2 | 69 | 1452s | $10.58 | 484/484 |
| 3 | 67 | 337s | $2.44 | 484/484 |
| **Avg** | **67** | **745s** | **$5.65** | **3/3** |

Run 2 is a 24-minute outlier — a debugging spiral on a logic bug.
This is the variance signature of behavioral-only debugging: one bad
reasoning path costs 10x. Median is 445s. Forced kondo (E12a) avoids
this class of blowup.

#### E12 summary — all tracks, best tooling

| Track | Avg wall | Per-bug | Static errors | Bugs |
|-------|----------|---------|---------------|------|
| **Python + mypy** | **255s** | **8.5s** | 16 | 30 |
| Beagle + cheatsheet | 328s (median 299s) | 9.4s (median 8.5s) | 20 | 35 |
| Beagle combined | 347s* | 9.9s | 20 | 35 |
| Beagle E10 (emit-patch) | 310s | 8.9s | 20 | 35 |
| E12a Clojure + kondo | 365s | 10.4s | 5 | 35 |
| Python E8 (no mypy) | 346s | 11.5s | — | 30 |
| Beagle E9 | 421s | 12.0s | 20 | 35 |
| E12b Clojure + kondo | 745s (median 445s) | 12.7s | 5 | 35 |
| Clojure E8 (no kondo) | 485s | 13.9s | — | 35 |

*single run

### E4 Python — No-oracle correctness (13 modules, 7.2K LOC, 30 bugs, NO test oracle)

The most important missing experiment. E4 showed Beagle 3/3 vs Clojure
0/3 without a test oracle — Beagle's strongest result. Does Python +
mypy also produce correct code without tests?

The agent receives buggy code and mypy. No verify script. We score
correctness after the fact using the hidden 484-assertion oracle.

| Run | Turns | Wall | Cost | Score |
|-----|-------|------|------|-------|
| 1 | 50 | 411s | $3.65 | 484/484 |
| 2 | 50 | 357s | $3.69 | 484/484 |
| 3 | 50 | 319s | $3.50 | 484/484 |
| **Avg** | **50** | **362s** | **$3.61** | **3/3** |

**Python + mypy: 3/3 correct without an oracle.** All 30 bugs found and
fixed via mypy + code reading alone.

This reframes E4: the correctness divergence was Beagle vs *untyped
Clojure*, not Beagle vs *typed languages*. Any language with a static
type checker achieves the same no-oracle correctness. Clojure failed
because it lacks static checking, not because it lacks Beagle.

| Track | No-oracle correctness | Static checker |
|-------|-----------------------|----------------|
| Beagle E4 | 3/3 | beagle-check (20 errors) |
| Python + mypy | 3/3 | mypy (16 errors) |
| Clojure E4 | 0/3 | none |

---

## Progression narrative

```
E3b  (1.2K LOC)  → beagle 36% faster, both correct
E4   (8.5K LOC)  → wall time equal, beagle 3/3 vs clojure 0/3
E8   (full oracle) → beagle 23% faster, both correct
E9   (+ repair tools) → beagle 29% faster, 36% fewer tokens
E10  (+ emit-patch) → beagle 33% faster
E11  (model tier) → advantage scales: 33% Opus, 4% Sonnet, 2% Haiku
Python E8          → no mypy used; type system alone ≈ beagle E9
E12  (fair tools)  → Python+mypy fastest; beagle matches per-bug; prompt engineering matters
E4 Python          → 3/3 no-oracle correctness; E4 is a static-typing result, not a beagle result
```

## Key takeaways

1. **Correctness divergence is about static typing, not Beagle.** At
   8.5K LOC without a test oracle: Beagle 3/3, Python + mypy 3/3,
   Clojure 0/3. Any static type checker prevents the no-oracle
   correctness failures. Clojure fails because it lacks static
   checking, not because it lacks Beagle.

2. **Static type checking helps ~25% regardless of language.** Python
   with mypy is 26% faster than Python without (255s vs 346s). Beagle
   with checker is 23% faster than Clojure without (375s vs 485s).
   Clojure with kondo is 25% faster than without (365s vs 485s). The
   benefit of front-loading static diagnostics is consistent across
   ecosystems.

3. **Per-bug, Beagle and Python + mypy are close.** Median per-bug:
   Beagle 8.5s, Python 7.7s. The gap is narrow but Python's floor is
   lower. High variance on both tracks (Beagle 271–413s, Python 170–362s)
   means individual runs overlap significantly.

4. **Prompt engineering matters more for unfamiliar languages.** Beagle
   with a cheatsheet (328s avg) is 12% faster than without (375s). The
   cheatsheet partially compensates for training data disadvantage, but
   doesn't eliminate it. Notably, the combined "kitchen sink" prompt
   (347s) was slower than the simple cheatsheet — more instructions can
   hurt.

5. **Python is the fastest track for agent bug repair.** With mypy
   forced, Python averages 255s — faster than every Beagle configuration
   including emit-patch (310s). Python's advantages: training data
   familiarity, clear error messages, no compile step. This is specific
   to bug repair with a behavioral oracle; the no-oracle correctness
   story (E4) has not been tested for Python.

6. **Beagle's strongest case is within the Clojure ecosystem.** Beagle
   + cheatsheet (299s) beats the best Clojure configuration (365s) by
   18%. `beagle-check` catches 20 type errors vs clj-kondo's 5. If
   you're in Clojure, Beagle is a strict improvement.

7. **Caveat: Beagle's speed advantage needs behavioral coverage.**
   E4 showed value *without* oracle coverage (correctness, not speed).
   But for the *speed* advantage, coverage matters: with a partial oracle
   (E8 run 1, 291 assertions), Clojure was 2x faster because it could
   ignore untested broken code. Beagle's type checker forces the agent
   to fix all type errors regardless.

## Appendix: confounds and limitations

- **Python has fewer bugs.** 30 vs 35 — 5 Clojure-specific patterns
  don't translate. Per-bug normalization partially accounts for this.
- **Training data asymmetry.** The agent (Claude) has vastly more Python
  in its training data than Clojure or Beagle. The cheatsheet experiment
  shows this matters (~20% improvement), but doesn't fully close the gap.
  A fair comparison would need a model trained equally on all three.
- **E12 Beagle cheatsheet is a single run.** The 299s result is
  directional. More runs needed to confirm.
- **E11 sample sizes are small.** Sonnet: 2 runs each. Haiku: 1 run
  each. Treat the sub-Opus numbers as directional, not conclusive.
- **E10 run 1 invalid.** Three `--emit-patch` toolchain bugs discovered
  during run 1; fixed before runs 2–3. Results use valid runs only.
- **E12 combined run confounded.** The combined run (347s) changed two
  variables simultaneously: switched from 154-line cheatsheet to 75-line
  distilled version AND added emit-patch/daemon/syntax. Cannot isolate
  which caused the slowdown.
- **LOC differs across tracks.** Beagle 8500 LOC, Clojure 4700 LOC,
  Python 7200 LOC. More code means more context to read, though bug
  density (bugs per LOC) is roughly comparable.
