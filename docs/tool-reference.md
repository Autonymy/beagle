# beagle — tool reference

Complete CLI tool catalog. See `CLAUDE.md` for behavioral policy on when
to use each tier.

## Structural checking (pass 0)

### `beagle-syntax`

Delimiter structure checker with four output layers.

```
beagle-syntax [--check|--repair|--edits|--ledger] FILE ...
```

| flag | description |
|---|---|
| `--check` | Validate delimiter structure (default) |
| `--repair` | Auto-fix delimiter structure |
| `--edits` | Print repair edits as JSON |
| `--ledger` | Show structural event ledger |
| `--json` | Machine-readable JSON output |
| `--diff` | Show edits without applying |
| `--write` | Write repaired file in place |
| `--emit-patch` | Output unified diff from repair |
| `--around N` | Center ledger around line N |

Output layers:
1. **Human summary** (`--check`): file hash, first error detail, stack at error, delimiter counts with UNBALANCED flags
2. **Structural ledger** (`--ledger`): per-delimiter-event table (line:col, depth before→after, event type, stack, line hash, context)
3. **Machine JSON** (`--check --json`): structured object with status, errors, counts, stack_at_first_error, repair patch
4. **Unified diff** (`--repair --emit-patch`): `git apply` compatible patch

## Type checking

### `beagle-check`

Type-check a single file.

```
beagle-check <source.bclj|.bjs|.bnix|.bcljs>
```

### `beagle-check-all`

Batch type-check files or directories.

```
beagle-check-all <file-or-dir> ...
```

## Compilation

### `beagle-build`

Compile a single file to its target language.

```
beagle-build <source.bclj|.bjs|.bnix|.bcljs> [output]
```

### `beagle-build-all`

Batch compile files or directories.

```
beagle-build-all <file-or-dir> ... [--out <dir>]
```

## Auto-fix

### `beagle-fix`

Auto-fix high-confidence type errors.

```
beagle-fix [--dry-run|--apply] <file-or-dir> ...
```

| flag | description |
|---|---|
| `--dry-run` | Show what would be fixed without applying |
| `--apply` | Apply high-confidence fixes in-place |

## Query tools

All query tools accept `<file-or-dir> ...` and search recursively.
The daemon accelerates these — use `beagle-daemon query <cmd> ...`
for cached results.

### `beagle-sig`

Print a function's typed signature.

```
beagle-sig <fn-name> <file-or-dir> ...
```

### `beagle-fields`

Print record fields, types, and accessors.

```
beagle-fields <RecordName> <file-or-dir> ...
```

### `beagle-callers`

Find all call sites of a function.

```
beagle-callers <fn-name> <file-or-dir> ...
```

### `beagle-provides`

List module exports.

```
beagle-provides <file-or-dir> ...
```

### `beagle-impact`

Callers + change impact analysis.

```
beagle-impact <fn-name> <file-or-dir> ...
```

## Macro expansion

### `beagle-expand`

Show source after macro expansion.

```
beagle-expand <source.bclj|.bjs|.bnix|.bcljs>
```

## Daemon

### `beagle-daemon`

Persistent query server. Keeps parsed ASTs cached, eliminates Racket
startup (~0.33s) and re-parse (~1.6s) per tool call. Supports inotify
file watching for ~100ms re-check.

```
beagle-daemon <command> [args...]
```

| command | description |
|---|---|
| `start` | Start daemon in background (TCP, ephemeral port) |
| `start --watch <dir>` | Start with inotify file watcher |
| `stop` | Stop running daemon |
| `status` | Check if daemon is running (JSON) |
| `query <cmd> <args>` | Send query to running daemon |

Queries (returns JSON):

| query | description |
|---|---|
| `sig <fn-name> <dir>` | Typed signature |
| `fields <record> <dir>` | Record fields |
| `callers <fn-name> <dir>` | Call sites |
| `provides <file>` | Module exports |
| `impact <fn-name> <dir>` | Change impact |
| `check <dir>` | Type check |
| `check-enriched <dir>` | Full type check + enriched context |
| `check-result [file]` | Latest pre-computed result from watcher |
| `latest-results` | All results since last query (clears buffer) |
| `watch <dir>` | Start inotify watcher |
| `unwatch` | Stop all watchers |
| `invalidate [file]` | Invalidate AST cache |
| `ping` | Connectivity check |

Environment variables:
- `BEAGLE_DAEMON_PORTFILE` — port file path (default: `/tmp/beagle-daemon.port`)
- `BEAGLE_DAEMON_PIDFILE` — PID file path (default: `/tmp/beagle-daemon.pid`)

## LSP

### `beagle-lsp`

Language Server Protocol server (JSON-RPC 2.0, Content-Length framing).

```
beagle-lsp
```

Capabilities: hover, diagnostics, document symbols, jump-to-definition, completion.

## REPL

### `beagle-repl`

Typed REPL with persistent environment. Parse → check → emit per input.

```
beagle-repl
```

Commands: `:type`, `:sig`, `:env`, daemon integration.

## Repair compiler

Multi-stage repair pipeline for when the type checker isn't enough.

### `beagle-repair`

Unified repair pipeline with auto mode.

```
beagle-repair <source-dir> <verify-script> [--auto] [--threshold 0.85] [--emit-patch]
```

| flag | description |
|---|---|
| `--auto` | Apply fixes automatically above threshold |
| `--threshold N` | Confidence threshold (default: 0.85) |
| `--emit-patch` | Output unified diff |

### `beagle-blame`

Semantic property rules + static suspicion analysis.

```
beagle-blame <source-dir> <verify-script>
```

### `beagle-specfix`

9 candidate strategies: accessor swap, arg permutation, cross-evidence.

```
beagle-specfix <build-dir> <verify-script>
```

### `beagle-trace`

Per-assertion arithmetic trace, source location correlation, call-graph walk.

```
beagle-trace <build-dir> <verify-script> [--focus <fn-name>]
```

### `beagle-cascade`

Call graph impact, predictive blame, root cause detection.

```
beagle-cascade <source-dir> <verify-script> [--modified fn1,...] [--from-failures]
```

## Testing tools

### `beagle-proptest`

Record generators, return-type property inference, differential testing, shrinking.

```
beagle-proptest <source-dir> [--run] [--build-dir <dir>]
```

### `beagle-oracle`

Golden snapshot, assertion generation, differential mode.

```
beagle-oracle <golden-source-dir> [--out FILE] [--diff <modified-dir>]
```

### `beagle-muttest`

13 mutation operators, coverage gap reports.

```
beagle-muttest <build-dir> <verify-script> [--limit N]
```

## Distributed tracing

### `beagle-dtrace`

Instrument, collect, view, blame, and graph distributed traces.

```
beagle-dtrace <subcommand> [args...]
```

| subcommand | description |
|---|---|
| `instrument <build-dir> [--services s1,s2,...] [--out <dir>]` | Add tracing to build |
| `collect [--port N] [--dir <trace-dir>]` | Collect trace data |
| `view <trace-dir> [--trace-id <id>]` | View traces |
| `blame <trace-dir> [--trace-id <id>] [--verify <script>]` | Blame analysis |
| `graph <trace-dir>` | Service dependency graph |
| `cascade <trace-dir> [--trace-id <id>]` | Cascade failure analysis |

## Maintenance

### `beagle-docs-sync`

Propagate codebase facts (test count, stdlib size, type names) into docs.

```
beagle-docs-sync [--dry-run] [--verbose]
```

### `beagle-js-coverage`

JS target stdlib coverage report. Shows visible/emitted/runtime helper counts.

```
beagle-js-coverage
```

### `beagle-smap`

Source map composition for CLJS target.

```
beagle-smap extract <file.cljs>              # stdout JSON mapping
beagle-smap compose <js.map> <mapping.json>  # rewritten source map
```

### `beagle-verify-enriched`

Verify enriched type-check results against build output.

```
beagle-verify-enriched <build-dir> <verify-script>
```

## Project bootstrap

### `beagle init`

Bootstrap a beagle project with language context.

```
beagle init [--claude-code] [target-dir]
```

`--claude-code` generates PostToolUse hook, settings, `CLAUDE.md`, and
language context file. Start the daemon after init:

```
beagle-daemon start --watch .
```
