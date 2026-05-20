# Feature: Task Groups

## Summary

Tasks can optionally belong to a named **group**. If any task in a group
fails to schedule (for any reason), all remaining unscheduled tasks in
that same group should also fail with a `GroupFailure` reason.

This models atomic batches — "either all of these tasks get scheduled, or
none of them should."

## Requirements

1. Add a `group` field to the `Task` record. It is a `String` — an empty
   string `""` means the task is ungrouped.

2. Add a `GroupFailure` record to the `FailureReason` union:
   ```
   (defrecord GroupFailure [(group-id : String) (failed-task-id : String)])
   ```
   - `group-id`: the group that failed
   - `failed-task-id`: the task whose failure triggered the group failure

3. When scheduling, if a task fails and it belongs to a group, mark all
   remaining unscheduled tasks in the same group as failed with
   `GroupFailure`.

4. Update all `FailureReason` match statements in `errors.bgl` to handle
   `GroupFailure`:
   - `describe-failure-reason` should return a string like
     `"Group G1 failed due to task T2"`
   - `failure-reason-category` should return `"group"`
   - `failure-is-structural?` should return `false`

5. Tasks that are NOT in any group (`""`) are unaffected by this feature.

6. Tasks that are in a group but whose group has not failed should
   schedule normally.

7. Update `make-task` and `make-simple-task` constructors to accept/default
   the new field. `make-simple-task` should default `group` to `""`.
