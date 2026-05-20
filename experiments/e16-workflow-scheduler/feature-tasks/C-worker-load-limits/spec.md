# Feature: Worker Load Limits

## Summary

Workers can have a **maximum number of tasks** they can be assigned in a
single schedule. Once a worker reaches their limit, the scheduler must
not assign additional tasks to them.

This models shift-based capacity — "this worker can handle at most N
tasks today."

## Requirements

1. Add a `max-tasks` field to the `Worker` record. It is an `Int` — a
   value of `0` means the worker has no limit (unlimited tasks).

2. Add a `WorkerOverloaded` record to the `FailureReason` union:
   ```
   (defrecord WorkerOverloaded [(worker-id : String) (max-tasks : Int)])
   ```
   - `worker-id`: the worker that is at capacity
   - `max-tasks`: the worker's task limit

3. When scheduling, the scheduler must track how many tasks have been
   assigned to each worker. A worker who has already been assigned
   `max-tasks` tasks (where max-tasks > 0) must not receive additional
   assignments.

4. If a task cannot be scheduled because all eligible workers have
   reached their load limit, the task should fail with `WorkerOverloaded`.
   Use the first eligible (by id ASC) overloaded worker's info in the
   failure record.

5. Update all `FailureReason` match statements in `errors.bgl` to handle
   `WorkerOverloaded`:
   - `describe-failure-reason` should return a string like
     `"Worker W1 overloaded: max 2 tasks"`
   - `failure-reason-category` should return `"overload"`
   - `failure-is-structural?` should return `false`

6. Workers with `max-tasks` of `0` are unaffected — they can be assigned
   any number of tasks, exactly as before.

7. Update `make-worker` constructor to accept the new field.
