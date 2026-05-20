# Feature: Exclusive Resources

## Summary

Resources can be marked as **exclusive**. An exclusive resource can only
be used by one task in the entire schedule — once a task is assigned that
requires an exclusive resource, no other task can use that resource,
regardless of time windows.

This models one-shot consumable resources — "once this tool is checked
out, it's unavailable for the rest of the day."

## Requirements

1. Add an `exclusive` field to the `Resource` record. It is a `Bool` —
   `false` means the resource uses normal capacity-based sharing.

2. Add a `ResourceLocked` record to the `FailureReason` union:
   ```
   (defrecord ResourceLocked [(resource-id : String) (locked-by : String)])
   ```
   - `resource-id`: the exclusive resource that is locked
   - `locked-by`: the task-id that locked the resource

3. When scheduling, the scheduler must track which exclusive resources
   have been locked (and by which task). When a task requires an
   exclusive resource that has already been locked by a previously
   scheduled task, the task fails with `ResourceLocked`.

4. A locked exclusive resource is unavailable for the entire schedule,
   not just during the locking task's time window. This is different from
   normal capacity checking which only considers overlapping windows.

5. Non-exclusive resources continue to work exactly as before (capacity-
   based concurrent sharing).

6. An exclusive resource with capacity > 1 still only allows ONE task
   total (exclusivity overrides capacity).

7. Update all `FailureReason` match statements in `errors.bgl` to handle
   `ResourceLocked`:
   - `describe-failure-reason` should return a string like
     `"Resource R1 locked by task T1"`
   - `failure-reason-category` should return `"resource-lock"`
   - `failure-is-structural?` should return `false`

8. Update `make-resource` constructor to accept the new field.
