# Bug 03: Missing edge case -- empty task list not handled

**Category:** Missing edge case

**Description:**
The scheduler has an early-return guard that handles the empty task list:
when no tasks are provided, it immediately returns a successful empty
schedule. The bug removes this guard, causing the code to fall through
into the dependency-graph and topological-sort logic with zero tasks.
Depending on the language implementation, this may produce an incorrect
error result, crash when iterating empty structures, or trigger
unexpected code paths.

**Mutation:**
- Python `scheduler.py`: remove the `if not tasks: return ScheduleOk(assignments=[])`
  guard (2 lines removed).
- Beagle `scheduler.bgl`: remove the `(if (empty? tasks) (->ScheduleOk [])` branch
  and its matching closing paren (3 lines changed).

**Expected oracle impact:**
When called with an empty task list, the scheduler no longer returns a
clean `ScheduleOk(assignments=[])`. Instead it enters the main scheduling
path with no tasks. In Python, `build_dependency_graph([])` produces an
empty graph and `topological_order` returns `[]`, so `schedule` may still
return `ScheduleOk([])` by coincidence. In Beagle the fallthrough
behavior differs: it may hit the empty-workers check first (returning
an error).
