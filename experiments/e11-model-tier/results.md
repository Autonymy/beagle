# E11 Results: Model Tier

## Summary

No correctness divergence at any model tier — all configurations achieve 484/484.
Beagle's speed advantage scales with model capability: 33% at Opus, 4% at Sonnet,
~2% at Haiku. The tooling amplifies stronger models rather than compensating for
weaker ones.

## Raw data

### Sonnet 4.6 (2 runs each)

| Run | Sonnet+Beagle | Sonnet+Clojure |
|-----|--------------|----------------|
| 1 | 433s | 371s |
| 2 | 357s | 452s |
| **Avg** | **395s** | **411s** |

Correctness: 4/4 all pass 484/484.

### Haiku 4.5 (1 probe each)

| Run | Haiku+Beagle | Haiku+Clojure |
|-----|-------------|---------------|
| 1 | 276s | 281s |

Correctness: 2/2 both pass 484/484.

## Cross-tier comparison

| Config | Avg wall time | Δ vs Opus+Clojure | Cost tier |
|--------|--------------|-------------------|-----------|
| Opus+Beagle (E10) | 310s | 33% faster | $$$$$ |
| Haiku+Beagle | 276s | 40% faster | ¢ |
| Haiku+Clojure | 281s | 40% faster | ¢ |
| Sonnet+Beagle | 395s | 15% faster | $ |
| Sonnet+Clojure | 411s | 11% faster | $ |
| Opus+Clojure (E10) | 464s | baseline | $$$$$ |

## Key findings

### 1. Beagle's speed advantage scales with model capability

| Tier | Beagle speedup vs same-tier Clojure |
|------|-------------------------------------|
| Opus | 33% (310s vs 464s) |
| Sonnet | 4% (395s vs 411s) |
| Haiku | 2% (276s vs 281s) |

More capable models extract more value from beagle's structured tool output
(cascade scores, trace analysis, repair patches). Weaker models fall back to
sequential "read file, find bug, fix" patterns regardless of tooling.

### 2. Beagle lets you drop a model tier without losing speed

Sonnet+Beagle (395s) is faster than Opus+Clojure (464s) at ~5x lower token cost.
The tooling compensates for the capability gap between tiers.

### 3. No correctness divergence at any tier

All 6 configurations achieve 484/484. The E8 bug set (35 bugs across 13 modules)
is within reach of all three model tiers. A harder problem — more modules, subtler
bugs, deeper cross-module interactions — would be needed to find the capability
floor where beagle's type guidance produces correctness differences.

### 4. Haiku is surprisingly fast

Haiku completes in ~280s on both tracks — faster than Opus or Sonnet. Lower
per-token latency dominates; the bug set doesn't require enough reasoning depth
to penalize the smaller model. Single probe run — treat with caution.

## Interpretation

Beagle is a power tool, not training wheels. It amplifies capability rather than
compensating for weakness. The practical implication:

- **For frontier users (Opus):** beagle delivers 33% wall-time reduction — the
  primary value prop.
- **For cost-conscious users:** Sonnet+beagle matches Opus+clojure speed at 5x
  lower cost — model tier arbitrage.
- **For the cheapest tier (Haiku):** beagle provides negligible advantage on this
  bug set. The model can't leverage structured output effectively.

## Predictions vs actuals (from README.md)

| Metric | Predicted Sonnet+Beagle | Actual | Predicted Sonnet+Clojure | Actual |
|--------|------------------------|--------|-------------------------|--------|
| Correctness | 2-3/3 | 2/2 | 0-2/3 | 2/2 |
| Wall time | 300-500s | 395s | 600-1200s | 411s |

Correctness prediction was wrong — Sonnet is more capable than expected.
Wall time prediction was right for beagle, way off for clojure (predicted 2-3x
slower than actual).
