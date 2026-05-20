# Type Surface Experiment Bugs

10 bugs for the E16-T experiments. All target the Beagle golden code.
Each bug is a single-line change stored as a sed command in a .sh file
for easy application and reversal.

## Bug Index

| ID | Category | File | Checker catches? | Description |
|----|----------|------|-----------------|-------------|
| 01 | off-by-one | matcher.bgl | No | Window overlap: `<` to `<=` |
| 02 | off-by-one | scheduler.bgl | No | Retry count: `<=` to `<` |
| 03 | type-confusion | scheduler.bgl | No | Priority sort reversed |
| 04 | type-confusion | scheduler.bgl | P2 (defscalar) | task-id/worker-id swap |
| 05 | missing-case | scheduler.bgl | No | Empty task list not handled |
| 06 | missing-case | scheduler.bgl | P2 (exhaustive) | Match arm removed |
| 07 | logic | graph.bgl | No | Dep edges reversed |
| 08 | logic | scheduler.bgl | No | Tie-breaking order wrong |
| 09 | error-handling | scheduler.bgl | No | Error swallowed |
| 10 | data-flow | scheduler.bgl | No | Reads end-time as start-time |

8/10 bugs are invisible to the checker (logic errors).
2/10 are type-catchable at P2 (structural checks).
This ratio tests whether types help even for logic bugs.

## Usage

```bash
# Apply bug:
source type-bugs/NN-name/apply.sh

# Revert bug (re-copy golden):
cp golden/beagle/*.bgl workspace/
```
