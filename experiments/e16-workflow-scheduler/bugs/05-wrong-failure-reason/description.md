# Bug 05: Wrong failure reason -- NoCapableWorker vs WorkerUnavailable swapped

**Category:** Error handling

**Description:**
When a task cannot be scheduled, the code diagnoses the failure reason.
Two of the possible reasons are `NoCapableWorker` (no worker has the
required capabilities) and `WorkerUnavailable` (capable workers exist but
none are available during the needed time window). The bug swaps these
two conditions: when no worker has the right capabilities, it reports
`WorkerUnavailable`, and when capable workers are all busy, it reports
`NoCapableWorker`.

**Mutation:**
- Python `scheduler.py`: in `_build_failure`, the `if not cap:` branch
  now returns `WorkerUnavailable()` (with defaults) instead of
  `NoCapableWorker(...)`, and the final else branch returns
  `NoCapableWorker(...)` instead of `WorkerUnavailable(...)`.
- Beagle `matcher.bgl`: in `find-failure-reason`, the `(empty? cap-workers)`
  branch now returns `->WorkerUnavailable` instead of `->NoCapableWorker`,
  and the `(empty? avail)` branch returns `->NoCapableWorker` instead of
  `->WorkerUnavailable`.

**Expected oracle impact:**
The schedule output is structurally the same (same tasks fail), but the
failure reason enum/variant is wrong. Any oracle or test that checks the
*reason* field on `ScheduleFailure` will detect a mismatch. The failure
reason `NoCapableWorker` should only appear when no worker has the needed
capabilities; `WorkerUnavailable` should only appear when capable workers
are all busy. This bug inverts those semantics.
