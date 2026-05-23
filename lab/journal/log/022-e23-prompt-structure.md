# 022 — E23: prompt structure matters more than prompt content

**Date:** 2026-05-22
**Experiment:** E23 (feature sprint, prompt variant comparison)

## Hypothesis

Agents fail to use beagle's tools because the prompt buries them or
presents them wrong. Five structural variants — corrected baseline,
decision table, worked example, tools-first ordering, full operating
policy — should reveal the best framing.

## Experiment

Same task across all variants: build a 56-test workflow engine in
beagle/clj. Five prompt structures (A–E) tested in parallel, plus
Clojure controls from earlier runs. Single run per variant.

## Result

| Variant | Time | Tests |
|---------|-----:|:-----:|
| Clojure control | 258–336s | 56/56 |
| A: Flat reference + workflow | 443s | 56/56 |
| C: Worked example trace | 583s | 56/56 |
| B: Error→tool decision table | 682s | 56/56 |
| D: Tools-first ordering | >980s | 0/56 |
| E: Full operating policy | >983s | 0/56 |

## Interpretation

The signal is in the failures. D and E — the variants with the most
behavioral scaffolding — never wrote a line of code in 15+ minutes.
The agent got stuck satisfying preconditions and reading references
instead of building. More rules to internalize = later first action.

Among the three that completed, the flattest prompt (A) won. Each
layer of structural elaboration (worked example, routing table) added
~100-140s. The pattern is monotonic: less framing overhead, faster
delivery.

Caveat: n=1 per variant, parallel execution. The A>C>B ranking is
suggestive. The D/E failure at 0% is robust — that's not variance.

The original prompt and A are functionally identical (only a comment
header differs). Original took 1420–1641s in solo runs vs A's 443s
in the parallel batch — unexplained variance that underscores the
need for more runs if we want to rank A/B/C reliably.

## Next question

How compact can the winning prompt get? The Clojure control runs with
92 lines and no behavioral guidance. A uses 154 lines. Can we compress
the language reference to 1–3 paragraphs and still complete? That's
the compression experiment.
