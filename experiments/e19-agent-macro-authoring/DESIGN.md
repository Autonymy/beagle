# E19: Agent macro authoring

## Question

Can an LLM agent write procedural macros, given beagle-expand as feedback?

## Conditions

Three conditions, same task each time:

| Condition | Agent context | Feedback loop |
|-----------|--------------|---------------|
| **A: Baseline** | Hand-written by human (control) | Manual compile + run |
| **B: Prompted** | Agent with cheatsheet.md + proc macro docs | beagle-expand + compile + run |
| **C: Unprompted** | Agent with cheatsheet.md only (no proc macro docs) | beagle-expand + compile + run |

## Task

Write a proc macro `defrouter` that generates:
- A dispatch function mapping path strings to handler function names
- A middleware wrapper that logs requests
- A 404 fallback

Input: list of `(path handler-name)` pairs.

The task is chosen because:
1. It requires iterating over data (can't be a template macro)
2. It produces multiple top-level forms (tests `(Vec Form)` splicing)
3. The generated code has non-trivial types (String → String functions)
4. It's realistic (API routing is a common pattern)

## Spec change (mid-task)

After initial working version, change the spec:
- Add method matching: `(path method handler-name)` triples instead of pairs
- The dispatch function now checks both path and method

This tests propagation cost: how many sites need editing?
- Baseline: edit 3+ functions manually
- Macro: edit the macro body + call sites (data, not code)

## Metrics

| Metric | How measured |
|--------|-------------|
| Wall time | Start to passing `bb` execution |
| LOC | Non-blank, non-comment, non-`#lang` lines |
| Correctness | Output matches expected for 5 test cases |
| Iterations | Number of edit-compile-fix cycles |
| Spec change time | Time from spec change to passing execution |
| Spec change LOC delta | Lines added/removed for the change |

## Protocol

1. Start timer
2. Write the macro (or hand-written equivalent)
3. Compile with `racket FILE.bgl > out.clj`
4. Run with `bb out.clj`
5. If errors, use `beagle-expand FILE.bgl` to inspect expansion
6. Iterate until output matches expected
7. Stop timer, record metrics
8. Apply spec change, restart timer
9. Iterate until spec change passes
10. Stop timer, record metrics

## Expected output (pre-spec-change)

```
GET /users -> handle-users
GET /products -> handle-products
GET /unknown -> 404 Not Found
[LOG] GET /users
handle-users result
```

## Expected output (post-spec-change)

```
GET /users -> handle-users
POST /users -> create-user
DELETE /users -> 404 Not Found
[LOG] POST /users
create-user result
```

## Files

```
experiments/e19-agent-macro-authoring/
  DESIGN.md           — this file
  tasks/
    A-baseline.bgl    — human hand-written
    B-prompted.bgl    — agent with docs
    C-unprompted.bgl  — agent without proc macro docs
  results/
    RESULTS.md        — measurements + analysis
```
