# Bug 04: task-id / worker-id swap (type confusion)

**File:** scheduler.bgl, line 197-198 in schedule-with-retry
**Category:** Type confusion
**Checker catches:** P2+ (if defscalar distinguishes TaskId from WorkerId)

**Change:** Swap task-id and worker-id in Assignment constructor:
`(->Assignment (task-id task) (worker-id worker) ...)` →
`(->Assignment (worker-id worker) (task-id task) ...)`

**Effect:** Every assignment has task_id and worker_id swapped.
Oracle catches this immediately on any non-trivial schedule.
