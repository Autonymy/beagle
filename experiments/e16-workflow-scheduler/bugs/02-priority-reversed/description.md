# Bug 02: Priority sort reversed in topological order

**Category:** Type confusion / logic

**Description:**
The topological sort uses Kahn's algorithm with tie-breaking by
`(priority ASC, id ASC)`. Lower priority numbers mean higher urgency.
The bug negates the priority comparison so that tasks with higher priority
numbers (i.e. lower urgency) are scheduled first instead of lower numbers
(higher urgency). The topological ordering remains valid (dependencies are
still respected), but the tie-breaking among ready tasks is reversed.

**Mutation:**
- Python `graph.py`: negate `graph.task_priority.get(n, 0)` to
  `-graph.task_priority.get(n, 0)` in the two sort-key lambdas for
  queue construction and successor sorting.
- Beagle `graph.bgl`: change `[(task-priority task) tid]` to
  `[(- 0 (task-priority task)) tid]` in both sort-by keys in
  `topological-order`.

**Expected oracle impact:**
Tasks are scheduled in the wrong priority order. A priority-1 task that
should run before a priority-5 task will instead run after it (when both
are ready at the same time). Assignments still satisfy dependency
constraints but have suboptimal scheduling order, producing different
`start_time` / `worker_id` values.
