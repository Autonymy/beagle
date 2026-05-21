# E17 — Typed AST codegen: does structural generation surface help agents?

**Date:** 2026-05-21
**Codebase:** Heist (semantic UI compiler, ~/code/heist)
**Agent:** Claude Opus 4.6 via `claude` worktree-isolated agents

## Question

When an agent modifies a code generator (compiler emitter), does a
typed AST construction surface reduce mechanical errors and speed up
the work compared to raw string building?

E16 showed types help agents build features 24% faster on an 800-LOC
scheduler. E17 tests whether the same effect holds on a different
class of work: modifying a compiler that generates JavaScript via
string concatenation vs. typed AST node construction.

## Setup

Heist's `codegen.bjs` generates direct-DOM JavaScript from `.heist`
declarations. Two branches:

```
heist-string-baseline   — original string-concatenation codegen
heist-typed-ast-pilot   — inline JsExpr/JsStmt unions + DOM helpers
```

The pilot branch has:
- 17 typed records (JsIdent, JsLiteral, JsCall, JsMember, JsConst, etc.)
- 2 unions (JsExpr, JsStmt) enforced by beagle's type checker
- Typed emitter (emit-expr, emit-stmt) using exhaustive `match`
- 4 DOM helpers (dom-create, dom-set-prop, dom-append, dom-add-listener)
- One function converted (related-content-lines in gen-detail)

The baseline branch is identical except the AST infrastructure and
the converted function are reverted to string building.

Both branches:
- Have the beagle daemon running (PostToolUse hook on every .bjs edit)
- Pass 56/56 existing tests (34 CRM + 22 tracker)
- Differ only in whether codegen.bjs has the typed AST surface

## Protocol

For each feature task:
- Same prompt to both agents
- Worktree-isolated (no shared state)
- Same test suite as acceptance criterion

### Measurements

- Wall time to completion
- Number of tool uses (proxy for edit/check/test loops)
- Beagle checker catches (type errors surfaced by daemon)
- Build/test failures during development
- Final test pass rate
- Whether agent used the typed AST (pilot) or fell back to strings

## Feature tasks

### F1: Delete button on related records

Add a delete button (×) to each note item in the CRM detail panel.
Requires modifying DOM construction in gen-detail's render-related-fns.

Stresses: DOM construction, event handlers, store interaction, undo.

## Results

### F1: Delete button on related records

| Metric | Baseline (string) | Pilot (typed AST) |
|--------|-------------------:|-------------------:|
| Wall time | 336s | 371s |
| Tool uses | 38 | 29 |
| Checker catches | 0 | 0 |
| Build failures | 0 | 0 |
| Tests pass | 34/34 | 34/34 |
| Used typed AST | N/A | **No** |

**Null result.** Both agents produced clean, working implementations
using string concatenation. The pilot agent did not use the typed AST.

### Why F1 didn't test the thesis

The typed AST covers `related-content-lines` (tab container
construction). The feature required modifying `render-related-fns`
(item rendering loop) — a different function that is string-based on
both branches. The agent on the pilot branch saw the typed AST in the
same file but had no reason to use it, because it was editing a
section that was already string-based.

This is a task selection failure, not a typed-AST failure. The
experiment needs a feature that forces the agent to modify or extend
the converted section directly.

## Design notes for F2

A valid retest needs a feature that:
1. Requires modifying `related-content-lines` (the typed section), OR
2. Requires adding a new section to gen-detail using the same pattern

Candidates:
- **Inline editing of related record content** — modify the item
  rendering AND the tab container to support edit-in-place. This
  touches both the typed and untyped sections.
- **Add a second related entity** — add a "tags" entity to
  crm-v2.heist. The agent must extend `related-content-lines` to
  build a second tab, directly exercising the typed AST builders.
- **Related record filtering/search** — add a search input to the
  related tab container, requiring new DOM nodes in the typed
  `related-content-lines` section.

The strongest design: a feature that requires adding new DOM nodes
inside `related-content-lines` and wiring them to event handlers. That
puts the agent directly in the typed-vs-string construction code.

## Cross-module checking gap

During setup, discovered that beagle's type checker does not enforce
union boundaries on cross-module `:as`-aliased calls. Same-file unions
work (`expected Expr, got Const`). Cross-module aliased calls are
treated as opaque.

This forced the inline approach (types defined in codegen.bjs itself)
rather than a shared js-ast.bjs module. Filed as upstream issue:

```
same-file: union boundary enforced
cross-module (:as alias): constructor treated as opaque, no enforcement
expected: cross-module union field checks still enforce variants
```

Three possible root causes (not yet diagnosed):
1. Name resolution — checker can't resolve alias to binding
2. Type export — union/record definitions don't cross module boundary
3. Design gap — cross-module inference never implemented

Diagnosis requires reading check.rkt. Deferred until the typed-AST
thesis is validated by experiment.
