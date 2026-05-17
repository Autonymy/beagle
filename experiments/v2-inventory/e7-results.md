# E7 Results: Re-running E4 with Optimized Prompts

## Experiment Design

- **Codebase**: 13-module inventory & order management system
- **Scale**: 8,570 LOC beagle / 4,759 LOC Clojure
- **Bugs**: 35 injected (20 caught by beagle type checker, 15 logic-only)
- **Oracle**: 484 assertions, generic labels (no bug IDs — unchanged from E4)
- **Agent**: Claude (Opus 4.7 via `claude -p`), unlimited iterations, tools: Read/Edit/Bash
- **Runs**: 2 per track (beagle, clojure)

### What changed from E4

E4 showed total correctness divergence (beagle 3/3, clojure 0/3). E7 re-runs E4's exact codebase and oracle with one variable changed: **prompt quality**.

| Factor | E4 (original) | E7 (this experiment) |
|--------|---------------|---------------------|
| Beagle prompt | Minimal (checker report only) | Full: cheatsheet + 20-error report + domain semantics + tool paths |
| Clojure prompt | Minimal ("fix bugs, run verify") | Full: domain semantics + module DAG + tool paths + verify instructions |
| Oracle | Same 484 assertions | Same 484 assertions |
| Buggy code | Same 35 bugs | Same 35 bugs |
| Agent model | Sonnet (via Agent tool) | Opus 4.7 (via `claude -p`) |
| Iteration limit | Unbounded | Unbounded |

Two confounds: (1) model upgrade (Sonnet → Opus), (2) prompt optimization applied to both tracks. These are intentional — E7 asks "does beagle still provide value when both sides are maximally optimized with a frontier model?"

## Results

| Run | Track | Score | Time (s) | Turns | Input Tokens | Output Tokens | Cost (USD) |
|-----|-------|-------|----------|-------|--------------|---------------|------------|
| 1 | beagle | 484/484 | 414 | 76 | 4,565,464 | 18,607 | $3.28 |
| 2 | beagle | 484/484 | 362 | 77 | 4,865,331 | 18,766 | $3.49 |
| 1 | clojure | 484/484 | 441 | 75 | 9,179,949 | 21,935 | $6.45 |
| 2 | clojure | 484/484 | 288 | 58 | 4,683,050 | 16,192 | $3.36 |

### Averages

| Track | Score | Avg Time | Avg Turns | Avg Input Tokens | Avg Output Tokens | Avg Cost |
|-------|-------|----------|-----------|------------------|-------------------|----------|
| beagle | 484/484 (100%) | 388s | 76.5 | 4,715,398 | 18,687 | $3.38 |
| clojure | 484/484 (100%) | 365s | 66.5 | 6,931,500 | 19,064 | $4.90 |

### Variance

| Track | Cost range | Time range | Turn range |
|-------|-----------|------------|------------|
| beagle | $3.28–$3.49 (6% spread) | 362–414s | 76–77 |
| clojure | $3.36–$6.45 (92% spread) | 288–441s | 58–75 |

## Analysis

### No correctness divergence

Both tracks achieved 100% on both runs. The optimized clojure prompt eliminated E4's 0/3 failure rate.

### Beagle's advantage is consistency, not correctness

Beagle shows **tight variance**: cost stays in a narrow band ($3.28–$3.49), turns are nearly identical (76–77), and the agent follows a predictable path (fix type errors → run verify → fix remaining logic bugs). The checker report front-loads 20 bugs with exact locations and fix hints.

Clojure shows **high variance**: one run costs $3.36, another costs $6.45 (nearly 2x). The agent's success depends on how efficiently it navigates the verify-read-fix loop. When it gets lucky (reads the right files first, guesses correct fixes), it's fast. When it doesn't, it burns tokens re-reading and re-running.

### Why E4's divergence disappeared

E4's clojure failures were caused by a combination of factors that E7 eliminated:

1. **Weaker model** (Sonnet vs Opus) — less able to reason about cross-module dependencies without type hints
2. **No domain semantics in prompt** — the clojure agent didn't know timestamps were seconds, amounts were cents, etc.
3. **No tool paths** — the agent couldn't easily invoke structural query tools
4. **No module DAG** — the agent didn't know dependency order, making cross-module bug fixing harder

The type checker advantage is real (20 bugs located instantly vs. discovered iteratively), but a well-prompted frontier model can compensate through brute-force iteration.

### Token economics: where beagle pays for itself

At current Opus pricing, the average cost gap is $1.52 per run ($4.90 - $3.38). Over many runs or in production CI where this runs on every commit, the savings compound. More importantly, beagle's low variance means predictable costs — no surprise $6+ bills from unlucky iteration paths.

The token gap (4.7M avg vs 6.9M avg, ~1.5x) comes from clojure needing to:
- Read the full 1316-line verify script to understand expected behavior
- Run verify multiple times to discover which bugs remain
- Re-read source files to understand context that beagle's checker report provides upfront

### Comparison to E4

| Metric | E4 beagle | E4 clojure | E7 beagle | E7 clojure |
|--------|-----------|------------|-----------|------------|
| Score | 3/3 (100%) | 0/3 (0%) | 2/2 (100%) | 2/2 (100%) |
| Model | Sonnet | Sonnet | Opus 4.7 | Opus 4.7 |
| Prompt quality | Basic | Minimal | Optimized | Optimized |

## Conclusions

1. **Beagle's value at scale is efficiency, not gating.** With a frontier model and good prompts, both tracks converge. Beagle gets there with less variance and ~30% lower average cost.

2. **E4's divergence was partly prompt/model-dependent.** The type checker's advantage is strongest when the agent is capability-limited (weaker model, poor prompts, context pressure). Frontier models with good prompts can brute-force through 35 bugs at 8.5K LOC.

3. **Consistency is the durable advantage.** Beagle turns: 76, 77. Clojure turns: 58, 75. Beagle cost: $3.28, $3.49. Clojure cost: $3.36, $6.45. For production use cases (CI, automated refactoring), predictability matters.

4. **The checker report is a high-value prompt artifact.** 20 errors with signatures, "did you mean?" hints, and exact line numbers is worth ~$1.50 in avoided iteration cost per run at this scale. At larger scales or with weaker models, the value increases.

5. **Next frontier**: to produce correctness divergence with Opus, likely need either (a) much larger scale (20+ modules, 15K+ LOC), (b) token/turn budgets that force efficiency, or (c) more subtle bug classes (semantic errors that the verify script only partially covers).
