# beagle behavior bench — scoreboard

Generated 2026-05-15 13:40:01.

Each response is compiled to Clojure, run against the task's
behavior verification script (`tasks/<task>.verify.clj`), and
timed end-to-end.

| response | variant | result | total ms |
|---|---|---|---|
| 01-greet-a-current | a-current | ✓ PASS | 544 |
| 01-greet-a-current-run-2 | a-current | ✓ PASS | 544 |
| 01-greet-a-current-run-3 | a-current | ✓ PASS | 576 |
| 01-greet-b-required | b-required | ✓ PASS | 553 |
| 01-greet-c-minimal | c-minimal | ✓ PASS | 577 |
| 01-greet-d-inline | d-inline | ✓ PASS | 546 |
| 01-greet-f-schema-inline | f-schema-inline | ✓ PASS | 582 |
| 10-macro-inc-a-current | a-current | ✓ PASS | 610 |
| 10-macro-inc-b-required | b-required | ✓ PASS | 574 |
| 10-macro-inc-f-schema-inline | f-schema-inline | ✓ PASS | 586 |
| 16-factorial-a-current | a-current | ✓ PASS | 578 |
| 16-factorial-a-current-run-2 | a-current | ✓ PASS | 530 |
| 16-factorial-a-current-run-3 | a-current | ✓ PASS | 613 |
| 16-factorial-a-current-run-4 | a-current | ✓ PASS | 591 |
| 16-factorial-a-current-run-5 | a-current | ✓ PASS | 535 |
| 16-factorial-b-required | b-required | ✓ PASS | 540 |
| 16-factorial-c-minimal | c-minimal | ✓ PASS | 636 |
| 16-factorial-d-inline | d-inline | ✓ PASS | 542 |
| 16-factorial-f-schema-inline | f-schema-inline | ✓ PASS | 578 |
| 18-map-double-a-current | a-current | ✓ PASS | 556 |
| 18-map-double-b-required | b-required | ✓ PASS | 547 |
| 18-map-double-c-minimal | c-minimal | ✓ PASS | 599 |
| 18-map-double-d-inline | d-inline | ✓ PASS | 542 |
| 18-map-double-e-schema | e-schema | ✓ PASS | 553 |
| 18-map-double-f-schema-inline | f-schema-inline | ✓ PASS | 587 |
| 19-nested-let-a-current | a-current | ✓ PASS | 549 |
| 19-nested-let-b-required | b-required | ✓ PASS | 591 |
| 19-nested-let-f-schema-inline | f-schema-inline | ✓ PASS | 640 |
| 21-boolean-ops-a-current | a-current | ✓ PASS | 520 |
| 21-boolean-ops-b-required | b-required | ✓ PASS | 517 |
| 21-boolean-ops-c-minimal | c-minimal | ✓ PASS | 581 |
| 21-boolean-ops-d-inline | d-inline | ✓ PASS | 564 |
| 21-boolean-ops-f-schema-inline | f-schema-inline | ✓ PASS | 622 |
| 22-multi-arg-macro-a-current | a-current | ✓ PASS | 602 |
| 22-multi-arg-macro-b-required | b-required | ✓ PASS | 553 |
| 22-multi-arg-macro-c-minimal | c-minimal | ✓ PASS | 617 |
| 22-multi-arg-macro-d-inline | d-inline | ✓ PASS | 609 |
| 22-multi-arg-macro-e-schema | e-schema | ✓ PASS | 606 |
| 22-multi-arg-macro-f-schema-inline | f-schema-inline | ✓ PASS | 559 |
| 25-cond-many-a-current | a-current | ✓ PASS | 538 |
| 25-cond-many-b-required | b-required | ✓ PASS | 551 |
| 25-cond-many-c-minimal | c-minimal | ✓ PASS | 616 |
| 25-cond-many-d-inline | d-inline | ✓ PASS | 597 |
| 25-cond-many-e-schema | e-schema | ✓ PASS | 597 |
| 25-cond-many-f-schema-inline | f-schema-inline | ✓ PASS | 531 |
| 26-compose-a-current | a-current | ✓ PASS | 580 |
| 26-compose-b-required | b-required | ✓ PASS | 610 |
| 26-compose-c-minimal | c-minimal | ✓ PASS | 600 |
| 27-sum-of-squares-a-current | a-current | ✓ PASS | 551 |
| 27-sum-of-squares-b-required | b-required | ✓ PASS | 531 |
| 27-sum-of-squares-c-minimal | c-minimal | ✓ PASS | 584 |
| 28-fizzbuzz-a-current | a-current | ✓ PASS | 552 |
| 28-fizzbuzz-b-required | b-required | ✓ PASS | 547 |
| 28-fizzbuzz-c-minimal | c-minimal | ✓ PASS | 563 |
| 29-gcd-a-current | a-current | ✓ PASS | 553 |
| 29-gcd-b-required | b-required | ✓ PASS | 926 |
| 29-gcd-c-minimal | c-minimal | ✓ PASS | 979 |
| 30-count-evens-a-current | a-current | ✓ PASS | 1012 |
| 30-count-evens-b-required | b-required | ✓ PASS | 841 |
| 30-count-evens-c-minimal | c-minimal | ✓ PASS | 634 |
| 31-fib-a-current | a-current | ✓ PASS | 578 |
| 31-fib-b-required | b-required | ✓ PASS | 591 |
| 31-fib-c-minimal | c-minimal | ✓ PASS | 592 |
| 32-any-positive-a-current | a-current | ✓ PASS | 597 |
| 32-any-positive-c-minimal | c-minimal | ✓ PASS | 592 |
| 33-my-range-a-current | a-current | ✓ PASS | 521 |
| 33-my-range-b-required | b-required | ✓ PASS | 569 |
| 33-my-range-c-minimal | c-minimal | ✓ PASS | 541 |

## Per-variant behavior pass rates

| variant | pass | total | rate |
|---|---|---|---|
| d-inline | 6 | 6 | 100.0% |
| a-current | 22 | 22 | 100.0% |
| c-minimal | 14 | 14 | 100.0% |
| e-schema | 3 | 3 | 100.0% |
| f-schema-inline | 8 | 8 | 100.0% |
| b-required | 15 | 15 | 100.0% |
