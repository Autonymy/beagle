# beagle behavior bench — scoreboard

Generated 2026-05-15 13:24:57.

Each response is compiled to Clojure, run against the task's
behavior verification script (`tasks/<task>.verify.clj`), and
timed end-to-end.

| response | variant | result | total ms |
|---|---|---|---|
| 01-greet-a-current | a-current | ✓ PASS | 545 |
| 01-greet-a-current-run-2 | a-current | ✓ PASS | 506 |
| 01-greet-a-current-run-3 | a-current | ✓ PASS | 553 |
| 01-greet-b-required | b-required | ✓ PASS | 525 |
| 01-greet-c-minimal | c-minimal | ✓ PASS | 579 |
| 01-greet-d-inline | d-inline | ✓ PASS | 559 |
| 01-greet-f-schema-inline | f-schema-inline | ✓ PASS | 545 |
| 10-macro-inc-a-current | a-current | ✓ PASS | 523 |
| 10-macro-inc-b-required | b-required | ✓ PASS | 516 |
| 10-macro-inc-f-schema-inline | f-schema-inline | ✓ PASS | 513 |
| 16-factorial-a-current | a-current | ✓ PASS | 513 |
| 16-factorial-a-current-run-2 | a-current | ✓ PASS | 530 |
| 16-factorial-a-current-run-3 | a-current | ✓ PASS | 529 |
| 16-factorial-a-current-run-4 | a-current | ✓ PASS | 555 |
| 16-factorial-a-current-run-5 | a-current | ✓ PASS | 552 |
| 16-factorial-b-required | b-required | ✓ PASS | 539 |
| 16-factorial-c-minimal | c-minimal | ✓ PASS | 524 |
| 16-factorial-d-inline | d-inline | ✓ PASS | 543 |
| 16-factorial-f-schema-inline | f-schema-inline | ✓ PASS | 552 |
| 18-map-double-a-current | a-current | ✓ PASS | 525 |
| 18-map-double-b-required | b-required | ✓ PASS | 564 |
| 18-map-double-c-minimal | c-minimal | ✓ PASS | 585 |
| 18-map-double-d-inline | d-inline | ✓ PASS | 556 |
| 18-map-double-e-schema | e-schema | ✓ PASS | 547 |
| 18-map-double-f-schema-inline | f-schema-inline | ✓ PASS | 518 |
| 19-nested-let-a-current | a-current | ✓ PASS | 585 |
| 19-nested-let-b-required | b-required | ✓ PASS | 554 |
| 19-nested-let-f-schema-inline | f-schema-inline | ✓ PASS | 543 |
| 21-boolean-ops-a-current | a-current | ✓ PASS | 530 |
| 21-boolean-ops-b-required | b-required | ✓ PASS | 527 |
| 21-boolean-ops-c-minimal | c-minimal | ✓ PASS | 527 |
| 21-boolean-ops-d-inline | d-inline | ✓ PASS | 547 |
| 21-boolean-ops-f-schema-inline | f-schema-inline | ✓ PASS | 542 |
| 22-multi-arg-macro-a-current | a-current | ✓ PASS | 563 |
| 22-multi-arg-macro-b-required | b-required | ✓ PASS | 525 |
| 22-multi-arg-macro-c-minimal | c-minimal | ✓ PASS | 541 |
| 22-multi-arg-macro-d-inline | d-inline | ✓ PASS | 528 |
| 22-multi-arg-macro-e-schema | e-schema | ✓ PASS | 547 |
| 22-multi-arg-macro-f-schema-inline | f-schema-inline | ✓ PASS | 543 |
| 25-cond-many-a-current | a-current | ✓ PASS | 559 |
| 25-cond-many-b-required | b-required | ✓ PASS | 568 |
| 25-cond-many-c-minimal | c-minimal | ✓ PASS | 636 |
| 25-cond-many-d-inline | d-inline | ✓ PASS | 552 |
| 25-cond-many-e-schema | e-schema | ✓ PASS | 548 |
| 25-cond-many-f-schema-inline | f-schema-inline | ✓ PASS | 578 |

## Per-variant behavior pass rates

| variant | pass | total | rate |
|---|---|---|---|
| d-inline | 6 | 6 | 100.0% |
| a-current | 14 | 14 | 100.0% |
| c-minimal | 6 | 6 | 100.0% |
| e-schema | 3 | 3 | 100.0% |
| f-schema-inline | 8 | 8 | 100.0% |
| b-required | 8 | 8 | 100.0% |
