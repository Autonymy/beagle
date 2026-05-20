# Feature: Resource Maintenance Windows

## Summary

Resources can have **maintenance windows** — time periods during which
the resource is completely unavailable (capacity effectively 0). This is
analogous to how workers already have `unavailable` windows.

## Requirements

1. Add a `maintenance-windows` field to the `Resource` record:
   ```
   (defrecord Resource [(id : String)
                        (name : String)
                        (capacity : Int)
                        (maintenance-windows : (Vec Window))])
   ```

2. Add a `ResourceMaintenance` record to the `FailureReason` union:
   ```
   (defrecord ResourceMaintenance [(resource-id : String)
                                   (window : Window)])
   ```
   - `resource-id`: the resource that was in maintenance
   - `window`: the conflicting maintenance window

3. Update the matcher to check resource maintenance windows. A task
   requiring a resource cannot be scheduled during that resource's
   maintenance window. The scheduler should slide past maintenance
   windows (same as it slides past worker unavailability).

4. Add a `ResourceInMaintenance` record to the `ViolationKind` union:
   ```
   (defrecord ResourceInMaintenance [(resource-id : String)
                                     (window : Window)])
   ```

5. Update the validator to check resource maintenance windows. If an
   assignment overlaps a maintenance window of a required resource,
   emit a `ResourceInMaintenance` violation.

6. Update all `FailureReason` match statements in `errors.bgl`:
   - `describe-failure-reason` should return something like
     `"Resource R1 in maintenance during [10, 20)"`
   - `failure-reason-category` should return `"maintenance"`
   - `failure-is-structural?` should return `false`

7. Update all `ViolationKind` match statements in `validator.bgl`:
   - `violations-by-kind` should map to `"resource-maintenance"`
   - `describe-violation` should produce a readable string

8. Update `make-resource` to accept the new field. Resources with
   empty maintenance windows `[]` behave identically to before.
