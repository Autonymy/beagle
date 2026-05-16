# v2: Inventory & Order Management — Experiment Framework

5-module, 1651 LOC beagle system with 15 typed records and ~100 functions.
Cross-module typed record access is the primary differentiator between beagle
and raw Clojure tracks.

## Module DAG

```
catalog (leaf)        customers (leaf)
   |                      |
   +--- inventory ---------+
   |         |             |
   +--- orders ------------+
             |
         reports (requires all)
```

## Experiments

### E1: Fresh Build
Agent builds all 5 modules from spec. Beagle vs Clojure, 3 runs each.

```bash
# Beagle track: compile + verify
bin/build-beagle responses/e1-beagle-run1
bin/verify-one responses/e1-beagle-run1/compiled

# Clojure track: verify directly
bin/verify-one responses/e1-clojure-run1
```

### E2: Cross-Module Refactoring
Add bulk discount feature across customers/orders/reports. Start from golden.

### E3: Bug Detection (12 injected bugs)
4 arity, 3 wrong accessor, 2 wrong type, 3 logic. Beagle catches 9/12 at
compile time.

```bash
# Verify buggy beagle catches type errors
bin/beagle-check buggy/beagle/inventory.rkt   # → arity error
bin/beagle-check buggy/beagle/orders.rkt      # → type mismatch
bin/beagle-check buggy/beagle/reports.rkt     # → arity error
```

## Verification

444 assertions across all 5 modules. Golden code passes 100%:

```bash
bin/verify-one golden/clojure   # 444 passed, 0 failed
```

## Directory Layout

```
spec/         — experiment specifications (given to LLM agents)
golden/       — reference implementations (beagle + compiled clojure)
buggy/        — E3 bug-injected versions (12 bugs, both tracks)
verify/       — master verification script (444 assertions)
responses/    — agent outputs per experiment run
bin/          — build, verify, and experiment runner scripts
```
