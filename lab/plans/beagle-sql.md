---
status: done
priority: —
---

# `beagle/sql` target

## Thesis

SQL is where LLMs burn the most tokens on mechanical failures — schema
drift, dialect confusion, spatial reasoning for nested subqueries. A
typed s-expression layer that catches column/type mismatches at compile
time before touching a database connection is exactly the feedback loop
compression that justifies Beagle's existence.

## Architecture

### Output model: query strings, not DDL

Beagle is a compiler, not a migration tool. beagle/sql emits SQL query
text. A beagle/clj or beagle/js program consumes that string through
its normal database driver. Beagle doesn't execute anything.

DDL generation (migrations from schema diffs) is a natural follow-on
but it's a separate tool, not the emitter's job — same way
`beagle-proptest` is adjacent to the compiler but not part of the
pipeline.

### Schema declarations = type environment

Schema declarations live in the beagle/sql file like `defrecord` but
relational — table name, columns, types, constraints. These are the
type environment, not the output.

```beagle
(deftable products
  [(id         : Int    :primary-key)
   (name       : String :not-null)
   (price      : Float  :not-null)
   (stock      : Int    :default 0)
   (category   : String?)
   (created_at : Timestamp :default :now)])

(deftable orders
  [(id         : Int :primary-key)
   (product_id : Int :references products.id)
   (quantity   : Int :not-null)
   (total      : Float)])
```

The type checker uses these declarations to validate queries. Foreign
key references (`products.id`) create join-compatibility constraints.

### Query forms

`select`, `insert`, `update`, `delete`, `with` (CTEs) compile to SQL
strings. The type checker validates column references, join
compatibility, and aggregate grouping against the declared schema.

```beagle
(select [p.id p.name (count o.id :as order-count)]
  (from products :as p)
  (left-join orders :as o (= o.product_id p.id))
  (where (> p.stock 0))
  (group-by p.id p.name))
```

Emits:
```sql
SELECT p.id, p.name, COUNT(o.id) AS order_count
FROM products AS p
LEFT JOIN orders AS o ON o.product_id = p.id
WHERE p.stock > 0
GROUP BY p.id, p.name
```

### What the type checker catches

- Column references to nonexistent columns → compile error
- Join on incompatible types (Int FK → UUID PK) → compile error
- `SELECT` column not in `GROUP BY` and not aggregated → compile error
- Insert with missing `:not-null` column (no default) → compile error
- Type mismatch in `WHERE` predicate (comparing String to Int) → compile error
- Reference to undeclared table → compile error

These are all errors that currently require a database round-trip to
discover. The daemon catches them in ~100ms.

### Dialect dispatch

Maps to the existing sub-target pattern:

```
#lang beagle/sql:pg       → PostgreSQL
#lang beagle/sql:sqlite   → SQLite
#lang beagle/sql:mysql     → MySQL
```

The emitter handles dialect differences:
- `RETURNING` clause: Postgres yes, MySQL no
- JSON operators: `->>`/`@>` (Postgres) vs `JSON_EXTRACT` (MySQL/SQLite)
- `LIMIT`/`OFFSET` syntax variations
- Type name mappings (`SERIAL` vs `INTEGER AUTO_INCREMENT`)
- String concatenation (`||` vs `CONCAT()`)

A portable `beagle/sql` (no dialect suffix) emits ANSI SQL and rejects
dialect-specific features at check time — same pattern as portable
beagle rejecting target-specific forms.

### Target-specific forms

Following the portability rule, all SQL forms are target-specific to
`beagle/sql` and gated via `TARGET-ONLY-FORMS` in check.rkt:

```
select, insert, update, delete       → DML
from, join, left-join, right-join    → table references
where, having, order-by, group-by   → clauses
limit, offset                        → pagination
with                                 → CTEs (overloads portable `with`? or `sql-with`?)
deftable                             → schema declaration
exists, in, between, like, case-when → predicates
coalesce, cast                       → expressions
```

Using `(select ...)` in a `beagle/clj` file → compile error:
`"select is only supported in beagle/sql"`.

### Potential query forms — full sketch

```beagle
;; Simple select
(select [name price]
  (from products)
  (where (= category "electronics"))
  (order-by price :desc)
  (limit 10))

;; Insert
(insert products
  [name price stock]
  (values ["Widget" 9.99 100]
          ["Gadget" 19.99 50]))

;; Insert from select
(insert products [name price stock]
  (select [name (* price 1.1) stock]
    (from archived_products)
    (where (> stock 0))))

;; Update
(update products
  (set [price (* price 0.9)]
       [stock (+ stock 10)])
  (where (= category "clearance")))

;; Delete
(delete (from orders)
  (where (< total 0)))

;; CTE
(sql-with
  [active-products
   (select [id name price]
     (from products)
     (where (> stock 0)))]
  (select [name price]
    (from active-products)
    (order-by price)))

;; Subquery in WHERE
(select [name]
  (from products)
  (where (in id
    (select [product_id]
      (from orders)
      (where (> quantity 5))))))

;; Window function
(select [name price
         (over (rank) (order-by price :desc) :as price-rank)]
  (from products))

;; Aggregate with HAVING
(select [category (avg price :as avg-price)]
  (from products)
  (group-by category)
  (having (> (avg price) 20.0)))
```

### Return type inference

The type checker should infer the result shape of a select:

```beagle
(select [p.name p.price]
  (from products :as p))
;; → inferred type: (ResultSet [{name : String} {price : Float}])
```

This enables downstream type checking if the query result is bound
in a let or passed to a function.

## Implementation layers

Following the standard form-addition pattern (parse → check → emit → test):

### parse.rkt

New AST nodes:
- `sql-table` (name, columns with types/constraints)
- `sql-select` (columns, from, joins, where, group-by, having, order-by, limit)
- `sql-insert` (table, columns, values-or-select)
- `sql-update` (table, set-pairs, where)
- `sql-delete` (from, where)
- `sql-join` (type, table, condition)
- `sql-cte` (bindings, body)
- Plus expression nodes for SQL-specific operators

### check.rkt

- Register all SQL forms in `TARGET-ONLY-FORMS`
- Schema validation: column existence, type compatibility
- Join validation: FK/PK type matching
- GROUP BY validation: non-aggregated columns must be grouped
- Return type inference for selects

### emit-sql.rkt

New emitter module. Dispatched via `emit-dispatch.rkt` for target `'sql`
(and sub-targets `'sql:pg`, `'sql:sqlite`, `'sql:mysql`).

### stdlib-sql.rkt

SQL-specific type declarations:
- Aggregate functions: `count`, `sum`, `avg`, `min`, `max`
- String functions: `upper`, `lower`, `trim`, `substring`, `concat`
- Date functions: `now`, `date_trunc`, `extract`
- Predicates: `is-null`, `is-not-null`, `between`, `like`, `ilike`
- Type casts: `cast`

### beagle.core.sql

No runtime needed — SQL is the runtime. Unlike beagle/js which needs
`beagle.core.js` for helper functions, SQL targets execute directly
in the database engine.

## Open questions

1. **`with` keyword collision.** Portable beagle has `with` for typed
   record update. SQL has `WITH` for CTEs. Options: `sql-with`, or
   context-sensitive dispatch (beagle/sql files interpret `with` as CTE).

2. **Parameterized queries.** Should beagle/sql emit `$1`/`?` placeholders
   for values, or inline literals? Parameterized is safer (SQL injection),
   but the output model needs to produce both the query string and a
   parameter list.

3. **Schema import.** Can beagle/sql `require` a schema file, so multiple
   query files share the same table declarations? This maps to the
   existing cross-module type import.

4. **Multi-statement output.** A beagle/sql file with multiple queries —
   does it emit one SQL string per top-level form, or concatenate with
   semicolons?

5. **Sub-target syntax.** Is `#lang beagle/sql:pg` parseable by Racket's
   reader, or does dialect need to be a `define-mode` / pragma?

## Prerequisites

- **Target-form gating** (`docs/plan-target-form-gating.md`) — the
  `TARGET-ONLY-FORMS` registry must exist before adding 15+ SQL-specific
  forms. Otherwise they'd be valid in beagle/clj files.

## Sequencing

1. Land target-form gating (prerequisite)
2. Schema declarations + select (minimum viable)
3. Insert/update/delete
4. Joins + join type checking
5. GROUP BY validation + aggregates
6. CTEs, subqueries, window functions
7. Dialect dispatch (pg/sqlite/mysql differences)
8. Parameterized query output
