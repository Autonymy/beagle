## Research

Frozen historical results from the lab. Methodology and raw data in the
companion lab repo.

| Question | Answer |
|---|---|
| E16: Do types make agents faster? | **24% faster** average, **45% on coordination-heavy features** (n=4). Same checker poorly-wired imposes 76% penalty — *integration matters as much as the type system*. |
| E18: Do proc macros compress code? | **2-3×** at realistic scale (crossover at 2-4 instances). Beagle template macros can't express the test patterns. |
| E19: Can agents write proc macros? | Yes, with docs (271s, 2 iterations). Without docs they invent runtime dispatch — proc macros need discoverability. |
| E3b: Beagle vs hand-written Clojure | **36% wall-clock improvement** on agent-driven authoring task. |
| E1-E15: vs Clojure / Python+mypy | Matches mypy correctness, beats Clojure correctness. mypy edges wall time — Beagle trades single-language speed for one typed surface across N backends. |

[Full lab](https://github.com/tompassarelli/beagle-lab) — E0–E22, methodology, raw results.
