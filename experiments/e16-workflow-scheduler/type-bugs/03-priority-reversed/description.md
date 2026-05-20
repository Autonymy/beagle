# Bug 03: Priority sort reversed (type confusion)

**File:** graph.bgl, topological-order sort-by
**Category:** Type confusion
**Checker catches:** No (both directions type-check as Int)

**Change:** Negate priority in sort key: `(task-priority task)` → `(- 0 (task-priority task))`

**Effect:** Higher-numbered priorities (lower urgency) get scheduled first.
Tasks with priority 1 should go before priority 5, but now priority 5 goes first.
