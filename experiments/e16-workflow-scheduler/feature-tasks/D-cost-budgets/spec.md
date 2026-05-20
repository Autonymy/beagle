# Feature: Task Cost Budgets

## Summary

Tasks have an associated **cost**, and the scheduler enforces a global
**budget ceiling**. Once the cumulative cost of scheduled tasks reaches
the budget, remaining tasks that would exceed it fail.

This models constrained resource allocation — "we can only spend X units
of effort in this scheduling window."

## Requirements

1. Add a `cost` field to the `Task` record. It is an `Int` — a value
   of `0` means the task is free (no cost).

2. Add a constant `max-total-cost` in `types.bgl`:
   ```
   (def max-total-cost : Int 1000)
   ```

3. Add a `BudgetExceeded` record to the `FailureReason` union:
   ```
   (defrecord BudgetExceeded [(task-cost : Int) (total-spent : Int) (budget : Int)])
   ```
   - `task-cost`: the cost of the task that could not be scheduled
   - `total-spent`: the total cost already committed
   - `budget`: the budget ceiling (max-total-cost)

4. When scheduling, the scheduler must track the cumulative cost of all
   successfully scheduled tasks. Before assigning a task, check whether
   `total-spent + task-cost > max-total-cost`. If so, the task fails
   with `BudgetExceeded`.

5. Tasks with `cost` of `0` never trigger budget failures (they are
   free). They do not count toward `total-spent`.

6. Update all `FailureReason` match statements in `errors.bgl` to handle
   `BudgetExceeded`:
   - `describe-failure-reason` should return a string like
     `"Budget exceeded: task costs 50, 980/1000 spent"`
   - `failure-reason-category` should return `"budget"`
   - `failure-is-structural?` should return `false`

7. Update `make-task` and `make-simple-task` constructors to
   accept/default the new field. `make-simple-task` should default
   `cost` to `0`.
