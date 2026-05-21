# Bug 01: Off-by-one in window overlap check

**Category:** Off-by-one

**Description:**
The interval overlap check uses `<` for the boundary condition (`start1 < end2`),
which correctly treats intervals as half-open `[start, end)`. The bug changes the
first comparison to `<=`, making the overlap check too loose: it now reports an
overlap when `start1 == end2`, i.e. when one interval ends exactly where the other
begins. These should be non-overlapping under half-open semantics.

**Mutation:**
- Python `matcher.py`: `start1 < end2` changed to `start1 <= end2`
- Beagle `matcher.bgl`: `(< start1 end2)` changed to `(<= start1 end2)`

**Expected oracle impact:**
Workers are incorrectly marked as unavailable when a task starts exactly when an
unavailability window ends. This causes valid schedules to fail with
`WorkerUnavailable`, or forces tasks to be pushed to later start times when they
could have started earlier.
