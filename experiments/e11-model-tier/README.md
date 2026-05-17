# E11: Model Tier (Sonnet)

**Hypothesis:** At lower model capability (Sonnet 4.6 vs Opus 4.6),
beagle's advantage widens — possibly including correctness divergence
that was absent at Opus tier.

E9 showed correctness is a tie at Opus 4.6. The question is: where on
the capability curve does beagle start providing correctness guarantees
the model can't achieve on its own?

If Sonnet + beagle matches Opus + Clojure on correctness, the story
becomes: "same correctness, 10x cheaper model."

**Baseline:** E9 (Opus 4.6) and E10 (Opus 4.6, compressed workflow).

## Setup

Same E8 system: 13 modules, ~8500 LOC, 35 injected bugs, 484 assertions.
Both tracks use the compressed workflow (beagle gets `--emit-patch`).

**Model:** Sonnet 4.6 (`--model sonnet`)

## Protocol

1. Copy buggy code into `trials/e11-beagle-run{1,2,3}/` and `trials/e11-clojure-run{1,2,3}/`
2. Run Claude Code with `--model sonnet --dangerously-skip-permissions`
3. Record: turns, wall time, output tokens, pass rate, **correctness** (assertions passed)
4. Compare against E9/E10 Opus baselines

## Key questions

1. **Correctness divergence?** Does Sonnet + Clojure fail to reach 484/484
   while Sonnet + beagle succeeds?
2. **Economics at lower tier?** Does the wall-time gap widen or narrow?
3. **Repair patch coverage?** Does `--emit-patch` help more at Sonnet
   (less capable model benefits more from mechanical automation)?
4. **Failure modes?** Where does Sonnet get stuck that Opus doesn't?

## Predictions

| Metric | Sonnet+Beagle | Sonnet+Clojure | Why |
|--------|--------------|----------------|-----|
| Correctness | 2-3/3 | 0-2/3 | Beagle's repair patch handles mechanical bugs Sonnet struggles with |
| Turns | ~80-120 | ~150+ | Sonnet needs more iterations without type guidance |
| Wall time | ~300-500s | ~600-1200s | Sonnet cheaper per token but needs more |
| Tokens | ~25-40K | ~60-100K | Without type errors, Sonnet explores more blindly |

The most interesting outcome: if Sonnet+beagle achieves 3/3 while Sonnet+Clojure
achieves 0-1/3, that's a correctness result that Opus masked.

## Run commands

```bash
# Beagle track
cd trials/e11-beagle-run1
claude --model sonnet --dangerously-skip-permissions -p "$(cat ../../spec/e11-beagle.md)"

# Clojure track
cd trials/e11-clojure-run1
claude --model sonnet --dangerously-skip-permissions -p "$(cat ../../spec/e11-clojure.md)"
```

## Cost estimate

Sonnet pricing is ~5x cheaper than Opus per token. At predicted token counts:
- Beagle: ~$0.30-0.50 per run × 3 = ~$1.50
- Clojure: ~$0.60-1.00 per run × 3 = ~$3.00
- Total: ~$4.50 (vs ~$15-20 for E9 at Opus pricing)
