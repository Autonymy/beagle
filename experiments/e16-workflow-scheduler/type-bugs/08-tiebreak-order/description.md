# Bug 08: Tie-breaking order wrong (logic error)

**File:** graph.bgl, topological-order sort-by
**Category:** Logic error
**Checker catches:** No (Vec of different orderings still type-check)

**Change:** In the sort key, swap priority and id:
`[(task-priority task) tid]` → `[tid (task-priority task)]`

**Effect:** Tasks are sorted by id first, then priority, instead of priority
first, then id. When two tasks have the same priority, the order is correct
(by id). But when they have different priorities, id takes precedence over
priority, producing wrong scheduling order.
