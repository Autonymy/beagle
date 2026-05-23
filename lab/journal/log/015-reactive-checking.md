# 015 — Reactive checking: inotify daemon + PostToolUse hook

**Date:** 2026-05-18
**Commit:** (pending)

## Hypothesis

Agents never discover beagle's diagnostic tools organically. Across 7
experiment runs: zero uses of `beagle-trace`, `beagle-cascade`,
`beagle-callers`, `beagle-impact`. Even `beagle-syntax` only appears
when the prompt explicitly prescribes it. The tools exist but agents
don't invoke them.

If we push diagnostics to the agent reactively — on every file edit,
not on every manual tool call — we eliminate the discovery problem
entirely and cut the edit→diagnose→fix loop from 3 tool calls to 0.

## What was built

1. **Daemon file watcher**: `filesystem-change-evt` (inotify) per .rkt
   file. On file change → invalidate AST cache → re-parse → full type
   check → semantic analysis → enrich errors with record field context.
   ~100ms per file on warm cache.

2. **Content hashing**: SHA1 of file at check time. Stale results
   discarded when hash doesn't match current file.

3. **New daemon commands**: `watch <dir>`, `check-enriched <dir>`,
   `check-result <file>`, `latest-results` (consumable buffer).

4. **Claude Code PostToolUse hook**: fires after every Edit/Write on
   .rkt files. Queries daemon for cached check result. Outputs 3–6
   lines of enriched diagnostics (error count, fix hints, record
   field context). Silent when file is clean.

5. **`beagle-verify-enriched`**: runs behavioral oracle, auto-diagnoses
   failures with trace (≤10 failures) and cascade (>5 failures).

## Expected impact

| Metric | Before (manual tools) | After (reactive) | Why |
|--------|----------------------|-------------------|-----|
| Tool calls to see errors | 1-2 per edit | 0 | Hook injects automatically |
| Record field lookups | 2-5 per session | 0 | Enrichment includes fields |
| Time to first error awareness | ~5s (run check) | ~200ms (hook delay) | inotify + cached check |
| Turns wasted on `beagle-fields` | ~3 per run | 0 | Context already present |

## What we don't know yet

- Does the hook injection actually reduce wall time, or does the extra
  context per turn slow down agent reasoning?
- Is 200ms sleep in the hook sufficient for the watcher to finish,
  or do we sometimes get stale results?
- Token budget: ~80 extra lines per session (20 errors × 4 lines).
  Is this net positive or does it crowd out reasoning context?

## Next experiment

**E13: reactive checking.** Same E8 system (13 modules, 35 bugs, 484
assertions). Compare:

- E12 beagle+cheatsheet (manual tools, 328s avg) as baseline
- E13 beagle+full prompt+daemon watch+hook (reactive)

If reactive beats manual, the system compounds with the full prompt.
If it doesn't, the hook is adding noise. Either way, the daemon
enrichment commands (`check-enriched`) are independently useful as
pull-mode tools even without the hook.
