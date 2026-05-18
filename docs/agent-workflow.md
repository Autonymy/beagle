# beagle agent workflow — tool routing decision tree

When fixing bugs in a beagle project, use this decision tree to choose
the right tool at each step. The goal: minimize reasoning cycles by
letting the toolchain do mechanical work.

## First move: start the reactive daemon

```bash
beagle-daemon start --watch .
beagle-fix --apply .
```

The daemon watches all .rkt files via inotify. Every time you edit a
file, it re-checks within ~100ms and (with the PostToolUse hook) injects
enriched diagnostics automatically — error count, fix hints, record
field context. This replaces manual `beagle-check-all` and most
`beagle-fields`/`beagle-sig` calls during the edit loop.

Then run `beagle-fix --apply .` to auto-fix mechanical type errors.

## Decision tree

```
Start
 │
 ├─ Daemon watching? Hook configured?
 │   YES → errors appear after each edit, no manual check needed
 │   NO  → run beagle-check-all . after each edit batch
 │
 ├─ Type errors remaining (from hook output or check-all)?
 │   YES → fix using the error output (fix hints, field context)
 │   NO  → compile and verify
 │
 ├─ Compile + verify
 │   Use beagle-verify-enriched for auto-diagnosis, or run verify directly
 │
 ├─ Many failures (>5)?
 │   YES → beagle-cascade --from-failures
 │         Fix highest cascade-score function FIRST
 │         (one fix may resolve 3-5 downstream failures)
 │   NO  → fix them individually
 │
 ├─ Trace shows wrong operator/operand?
 │   YES → swap the operator/operands at the traced source line
 │
 ├─ Trace shows wrong accessor (e.g., carrier-id where base-rate expected)?
 │   YES → the hook output already shows available fields
 │
 ├─ Trace shows wrong argument to a function?
 │   YES → check beagle-provides for expected parameter types
 │
 └─ No clear fix from trace?
     → Read the source, reason about the domain, make judgment call
```

## Tool selection cheat sheet

| Situation | Tool | What it tells you |
|-----------|------|-------------------|
| Starting a session | `beagle-daemon start --watch .` | Reactive checking on every edit |
| Auto-fix mechanical errors | `beagle-fix --apply .` | 6-8 fixes in zero reasoning |
| After editing a file | (automatic via hook) | Enriched errors with field context |
| Starting a repair session | `beagle-repair` | Full ranked queue with AUTO/SUGGEST |
| Logic bug, assertion fails | `beagle-verify-enriched` | Auto-diagnose with trace/cascade |
| Targeted trace | `beagle-trace --focus fn` | Exact operation + line that diverged |
| Many assertions failing | `beagle-cascade --from-failures` | Root cause(s) to fix first |
| Want to predict impact of a change | `beagle-cascade --modified fn1,fn2` | Which assertions will break |
| Need function signature | `beagle-sig fn-name src/` | Arg types and return type |
| Need record field types | `beagle-fields RecordName src/` | All fields with types + accessors |
| Need to know what module exports | `beagle-provides module.rkt` | Functions, records, types |
| Want oracle from golden code | `beagle-oracle golden/` | Auto-generated verify script |
| Comparing golden vs modified | `beagle-oracle golden/ --diff modified/` | Which functions diverge |

## Key principles

1. **Don't fix symptoms, fix roots.** If `beagle-cascade` shows a function
   with cascade score 5, fix that ONE function before touching the 5
   downstream failures it causes.

2. **Trust AUTO fixes.** They're oracle-verified (specfix) or mechanically
   determined (type checker with single suggestion). Apply them without
   reading the code.

3. **Use trace for logic bugs.** When the type system can't help (the code
   is type-correct but semantically wrong), `beagle-trace` shows the exact
   arithmetic chain. Look at the last operation in the trace — that's usually
   the bug.

4. **Rebuild after each fix batch.** Don't accumulate fixes without
   recompiling. The type checker might catch new issues exposed by earlier fixes.

5. **beagle-repair subsumes blame.** You rarely need to run `beagle-blame`
   directly — `beagle-repair` already includes blame evidence in the queue.
   Use blame standalone only for quick triage of a single failure.
