# Bug 05: Empty task list not handled (missing edge case)

**File:** scheduler.bgl, line 285
**Category:** Missing edge case
**Checker catches:** No

**Change:** Remove the early return for empty tasks. Delete lines:
`(if (empty? tasks) (->ScheduleOk []) ...)`
so the code always proceeds to build a dependency graph even with no tasks.

**Effect:** Empty input may crash or produce incorrect output depending on
how downstream functions handle empty collections.
