# Task: Fix bugs in a 13-module Python inventory system

You have a large inventory & order management system (13 modules, ~7200 LOC) written in Python with type annotations. The code has bugs. Your job is to fix ALL bugs so the behavioral verification script passes (484 assertions).

## Workflow — IMPORTANT: follow this order

Your primary diagnostic tool is **mypy**. Use it first, every time.

1. **Run mypy** to find type errors:
   ```
   nix-shell -p python3Packages.mypy --run "mypy *.py --ignore-missing-imports"
   ```
   Fix ALL mypy errors before proceeding. These are real bugs — wrong argument types, missing arguments, swapped fields. Each mypy error is a mechanical fix: read the error, fix the code, re-run mypy until clean.

2. **Run the verify script** to find remaining logic bugs:
   ```
   python3 verify.py
   ```

3. **Fix logic bugs** using verify output, then re-run verify until all 484 assertions pass.

4. **After every batch of fixes**, re-run mypy to make sure you haven't introduced new type errors, then re-run verify.

Do NOT skip mypy and go straight to verify. The mypy errors are structured, precise diagnostics — they tell you exactly which argument is wrong and what type was expected. Use them.

Note: mypy errors in `verify.py` itself can be ignored — they are Optional-narrowing noise, not bugs. Focus mypy on the 13 source modules only: `mypy catalog.py customers.py inventory.py orders.py reports.py shipping.py billing.py procurement.py promotions.py employees.py analytics.py audit.py notifications.py --ignore-missing-imports`

## Available tools

- `nix-shell -p python3Packages.mypy --run "mypy <files> --ignore-missing-imports"` — static type checking
- `python3 verify.py` — behavioral verification (484 assertions)
- `grep -n 'def function_name' *.py` — find function definitions
- `grep -rn 'function_name' *.py` — find all call sites

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
- Records are `@dataclass` — access fields directly (e.g., `product.unit_price`) or via accessor functions (e.g., `product_unit_price(product)`)
- Cross-module calls: `module.function()` (e.g., `cat.find_product_by_id(...)`)
- Integer division: `//` (Python's floor division)

## How to interpret mypy errors

```
billing.py:137: error: Missing positional argument "r" in call to "order_customer_id"  [call-arg]
```
→ `order_customer_id` expects an Order argument but got none. Find the call on line 137 and pass the order.

```
procurement.py:113: error: Argument 1 to "purchaseorder_supplier_id" has incompatible type "POLine"; expected "PurchaseOrder"  [arg-type]
```
→ Wrong record type passed. The function expects a PurchaseOrder, not a POLine. Fix the argument.

## How to interpret verify failures

```
FAIL: cat/product-margin Widget A
  expected: 700
  actual:   -700
```
→ `product_margin` returns -700 when it should return 700. Read the function, understand the computation, fix the logic.

## Important

- Run mypy FIRST. It catches ~16 type errors across 9 files — these are fast, mechanical fixes.
- After mypy is clean, the remaining bugs are logic errors (wrong operator, swapped operands, etc.) — use verify for those.
- The verify script is the final oracle — all 484 assertions must pass.
