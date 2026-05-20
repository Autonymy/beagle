# Bug 10: Reads end-time as start-time (data flow)

**File:** scheduler.bgl, line 87
**Category:** Data flow
**Checker catches:** No (both are Int fields on Assignment)

**Change:** In compute-earliest-start, read start-time instead of end-time
from dependency assignments:
`(assignment-end-time a)` → `(assignment-start-time a)` in the max-end reduce

**Effect:** Tasks start at the same time as their dependencies instead of
after them. Dependencies overlap with their dependents, violating the
dependency constraint.
