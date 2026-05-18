# 016 — Repair agent pool: abandoned

**Date:** 2026-05-18

## Hypothesis

E13 showed the reactive daemon collapses variance by eliminating "agent
gets lost" failures. But the agent still processes all errors sequentially
— when 15 type errors land at once, cognitive load causes poor decisions.
Splitting errors across parallel agents should reduce wall time by
keeping each agent focused on a smaller problem.

## What was built (E14)

**Architecture:** PostToolUse hook → error detection → agent pool dispatch

```
Agent 1 edits file → hook fires (~200ms)
  ├─ daemon check → errors found
  ├─ pool has capacity? → spawn repair agent
  │   └─ Agent 2 (headless claude -p) fixes errors independently
  │       └─ done → status relayed on Agent 1's next edit
  └─ Agent 1 continues working, never sees error details
```

**E14 result:** 0 pool activations across 3 runs. `claude -p
--dangerously-skip-permissions` bypasses PostToolUse hooks entirely.
+83 prompt lines caused +42% wall time regression (407s vs 287s).

## What was built (E15)

Four more approaches tested to force multi-agent edit delegation:

1. **Prompt-only fix plans** — agent told to write JSON fix plans to
   `.beagle/fixes/` instead of editing directly. Agent ignored
   instructions, used Edit.

2. **Remove Edit tool** (`--allowedTools "Read,Bash"`) — agent fell
   back to `sed -i` via Bash.

3. **MCP Edit proxy** (`beagle-pool-mcp`) — intercept Edit calls, queue
   to warm worker. Agent fell back to `sed` via Bash.

4. **CLI dispatch tool** (`dispatch-fix`) — Unix socket to warm pool,
   returns instantly. Offered as "faster than Edit." Agent used Edit
   directly. 0 dispatch-fix calls across 58 tool uses.

**Infrastructure built (all working):**
- `ClaudeSDKClient` warm workers: 1.2s per task after initial connect
- Unix socket dispatch: ~1ms round-trip
- Pool manager with cost caps, async dispatch, file watcher
- `dispatch-fix` CLI, `beagle-pool-mcp` MCP server

## Result

**0 pool activations across E14 (3 runs) + E15 (4 approaches).**

## Interpretation

Agents currently resist multi-agent edit delegation in ways that make this
an impractical optimization target. Tool-using behavior follows
availability and training, not prompt instructions:

- "NEVER use Edit" → agent uses Edit
- Remove Edit → agent uses sed
- Offer faster alternative → agent uses Edit
- MCP proxy intercept → agent uses sed

The pool infrastructure is sound (socket, warm workers, dispatch all
proven). The fundamental problem is at the agent behavior layer: there
is no reliable way to make an agent voluntarily delegate edits to
another agent when it can edit directly.

## Decision

**Abandon the repair agent pool.** The reactive daemon (E13, 287s avg)
is the ceiling for single-agent workflows. The inotify watcher and
enriched error injection stay — they're the actual source of E13's
variance collapse. The pool adds complexity with zero measured benefit.

Files kept for reference: `bin/beagle-pool`, `bin/beagle-pool-mcp`,
`bin/dispatch-fix`, `.beagle/repair-agent-prompt.md`.
