# Task: Fix bugs in a 13-module Python inventory system

You have a large inventory & order management system (13 modules, ~7200 LOC) written in Python with type annotations. The code has bugs. Your job is to find and fix ALL bugs.

**There is no test suite.** You must find bugs by reading the code, running mypy, and reasoning about correctness.

## Workflow

1. **Run mypy** to find type errors:
   ```
   nix-shell -p python3Packages.mypy --run "mypy catalog.py customers.py inventory.py orders.py reports.py shipping.py billing.py procurement.py promotions.py employees.py analytics.py audit.py notifications.py --ignore-missing-imports"
   ```
   Fix ALL mypy errors. These are real bugs — wrong argument types, missing arguments, swapped fields.

2. **Read the code** to find logic bugs that mypy can't catch:
   - Wrong operator (+ instead of -, * instead of /)
   - Swapped operands (a - b should be b - a)
   - Wrong field access (using `.id` where `.unit_cost` needed)
   - Wrong comparison direction (> should be <)
   - Wrong divisor (10 instead of 100 for percentages)

3. **After fixing**, re-run mypy to verify you haven't introduced new errors.

## Available tools

- `nix-shell -p python3Packages.mypy --run "mypy <files> --ignore-missing-imports"` — static type checking
- `grep -n 'def function_name' *.py` — find function definitions
- `grep -rn 'function_name' *.py` — find all call sites
- `python3 -c "import module; ..."` — test individual functions if needed

## Module dependency DAG (13 modules)

```
Layer 0 (leaves):     catalog, customers
Layer 1:              inventory (→ catalog)
                      orders (→ catalog, inventory, customers)
Layer 2:              reports (→ all layer 0–1)
Layer 3:              shipping (→ orders)
                      billing (→ orders, catalog)
                      procurement (→ catalog, inventory)
                      promotions (→ customers, orders)
                      employees (→ orders, catalog)
Layer 4:              analytics (→ orders, inventory, billing, shipping, catalog)
                      notifications (→ orders, customers, shipping, billing)
Layer 5:              audit (→ all modules)
```

## Domain semantics

- All monetary amounts are in **cents** (int). $12.00 = 1200
- Timestamps are epoch **seconds** (not millis)
- Days = seconds / 86400
- Percentages: integer 0-100
- Tax rates are decimal (0.15 = 15%)
- Records are `@dataclass` — access fields directly (e.g., `product.unit_price`) or via accessor functions
- Cross-module calls: `module.function()` (e.g., `cat.find_product_by_id(...)`)
- Integer division: `//` (Python's floor division)

## Important

- There is NO test script. You must find bugs through static analysis and code reading.
- mypy catches type/arity errors. Logic errors require reading the code carefully.
- Pay attention to function names — they hint at expected behavior (e.g., `product_margin` should return a positive margin, `reorder_needed` should check if stock is LOW).
- Cross-module calls are common — verify the arguments match the target function's signature.
- When you believe all bugs are fixed, say so and stop.
