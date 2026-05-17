# E6 Bug Catalog: 40 bugs across 6 modules

## Scalar-confusion bugs (18) — beagle catches, clojure cannot

| # | Module | Bug | What's wrong |
|---|--------|-----|-------------|
| 1 | orders | `create-order` passes `instrument-id` value where `account-id` expected | Swapped scalar args (both Long) |
| 2 | orders | `order-total` returns price directly instead of price*qty | Returns Price not Amount |
| 3 | orders | `validate-order` uses order-id value in balance check | OrderId where Amount expected |
| 4 | trades | `execute-trade` uses order-id value as trade-id | OrderId where TradeId expected |
| 5 | trades | `trade-pnl` uses exec-price where quantity expected in multiplication | Price where Quantity expected |
| 6 | trades | `settle-trade` uses trade-id to look up account | TradeId where AccountId expected |
| 7 | trades | `trade-net-amount` returns fee value instead of net | Amount confusion (fee vs net) |
| 8 | ledger | `record-trade` passes timestamp value where ref-id expected | Timestamp where Long expected |
| 9 | ledger | `entries-for-account` compares account-id to ref-id field | AccountId compared to wrong field |
| 10 | ledger | `net-flow` reads balance-after instead of amount field | Wrong Amount field |
| 11 | reports | `calculate-position` uses instrument-id value as quantity | InstrumentId where Quantity expected |
| 12 | reports | `portfolio-value` accumulates avg-price instead of current-value | Price where Amount expected |
| 13 | reports | `total-pnl` returns price-sum instead of amount-sum | Price where Amount expected |
| 14 | accounts | `credit-account` adds timestamp to balance instead of amount | Timestamp where Amount expected |
| 15 | accounts | `format-balance` uses account-id instead of balance for formatting | AccountId where Amount expected |
| 16 | instruments | `instrument-value` returns price (not price*qty) | Price where Amount expected |
| 17 | instruments | `instrument-total-cost` uses lot-size as fee-bps | Quantity where Long expected |
| 18 | orders | `total-exposure` sums order-ids instead of order totals | OrderId where Amount expected |

## Structural bugs (12) — beagle catches most, clojure catches some

| # | Module | Bug | What's wrong |
|---|--------|-----|-------------|
| 19 | trades | `execute-trade` called with 3 args (missing timestamp) | Wrong arity |
| 20 | orders | accesses `order-instrument-id` as `order-account-id` in filter | Wrong accessor |
| 21 | reports | `format-pnl-report` calls undefined `calculate-pnl` | Undefined ref |
| 22 | trades | `trades-for-account` uses `trade-instrument-id` instead of `trade-account-id` | Wrong accessor |
| 23 | ledger | `record-deposit` returns String annotation but builds LedgerEntry | Wrong return type |
| 24 | accounts | `transfer` calls `credit-account` with 3 args | Wrong arity |
| 25 | instruments | `update-price` uses `:ticker` field on wrong record | Wrong field in with |
| 26 | reports | `account-summary` calls `format-balance` with wrong arg count | Wrong arity cross-module |
| 27 | orders | `fill-order` doesn't set :filled-at field | With-completeness (missing field) |
| 28 | trades | `avg-exec-price` accesses quantity from price variable | Binding-accessor mismatch |
| 29 | ledger | `record-withdrawal` passes wrong record to account-id accessor | Wrong record type |
| 30 | reports | `generate-pnl-report` passes String where Timestamp expected | Type incompatibility |

## Logic bugs (10) — neither type system catches

| # | Module | Bug | What's wrong |
|---|--------|-----|-------------|
| 31 | trades | `trade-fee` uses 10 bps (0.1%) instead of 100 bps (1%) | Wrong constant |
| 32 | accounts | `sufficient-balance?` uses > instead of >= | Off-by-one comparison |
| 33 | accounts | `debit-account` adds instead of subtracts | Wrong arithmetic |
| 34 | trades | `trade-pnl` doesn't flip sign for sell orders | Wrong sign logic |
| 35 | orders | `validate-order` missing `(= side "buy")` check | Missing condition |
| 36 | orders | `pending-orders` returns cancelled orders too | Wrong filter predicate |
| 37 | ledger | `reconcile` uses <= instead of >= | Wrong comparison |
| 38 | reports | `calculate-position` counts sell qty as buy qty | Wrong accumulation |
| 39 | instruments | `price-change` divides by new price not old | Wrong denominator |
| 40 | accounts | `account-age-days` divides by 3600000 not 86400000 | Wrong divisor (hours not days) |
