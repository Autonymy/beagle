---
status: done
priority: —
---

# Racket Package Reorganization: composable lib/test/doc split

## Context

beagle is a single Racket package with `(define collection "beagle")` at the repo root. This means **everything** in the repo — `bin/`, `docs/`, `experiments/`, `.git/`, `flake.nix`, 39 CLI scripts, benchmark data — is technically in the Racket collection namespace. Per countvajhula's "How to Organize Your Racket Library," the fix is splitting into composable packages: `beagle-lib` (core), `beagle-test` (tests), `beagle-doc` (scribble), and `beagle` (aggregate).

This also separates dependency concerns: `rackunit-lib` only needed by tests, `scribble-lib`/`racket-doc` only needed by docs.

## Target layout

```
~/code/beagle/                      # repo root (NOT a package)
├── beagle-lib/                     # Core: compiler, stdlib, runtime
│   ├── info.rkt                    # (define collection "beagle")
│   ├── main.rkt
│   ├── lang/reader.rkt, reader-impl.rkt
│   ├── private/                    # 32 internal .rkt modules
│   ├── clj/, cljs/, js/, nix/, sql/, py/  # target dialects (main.rkt + lang/reader.rkt each)
│   └── lib/beagle/core.js, dtrace.clj     # runtime files
│
├── beagle-test/                    # Tests
│   ├── info.rkt                    # (define collection "beagle"), deps: beagle-lib rackunit-lib
│   └── tests/                      # 9 test .rkt + fixtures/
│
├── beagle-doc/                     # Documentation
│   ├── info.rkt                    # (define collection "beagle"), scribblings, deps: beagle-lib scribble-lib
│   └── scribblings/                # 10 .scrbl files
│
├── beagle/                         # Aggregate
│   └── info.rkt                    # (define collection 'multi), deps+implies
│
├── bin/                            # NOT in any collection (unchanged)
├── docs/                           # NOT in any collection (unchanged)
├── experiments/                    # NOT in any collection (unchanged)
├── examples/                       # NOT in any collection (unchanged)
├── runtime/                        # NOT in any collection (unchanged)
├── CLAUDE.md, AGENTS.md, README.md, flake.nix, ...
```

All three sub-packages declare `(define collection "beagle")` — Racket merges their disjoint file trees into one logical collection. `#lang beagle`, `#lang beagle/nix`, `(require beagle/private/parse)` all resolve correctly.

## info.rkt specifications

**beagle-lib/info.rkt** — core package, zero test/doc deps:
```racket
#lang info
(define collection "beagle")
(define deps '("base"))
(define version "0.8.0")
(define pkg-desc "Agent-native typed authoring layer — emits Clojure, ClojureScript, JavaScript, Nix, or SQL.")
(define pkg-authors '(tom))
(define license '(MIT))
(define raco-commands
  '(("beagle" beagle/private/raco-cmd "build, check, expand" #f)))
```

**beagle-test/info.rkt** — tests only:
```racket
#lang info
(define collection "beagle")
(define deps '("base" "beagle-lib"))
(define build-deps '("rackunit-lib"))
(define test-paths '("tests"))
```

**beagle-doc/info.rkt** — docs only:
```racket
#lang info
(define collection "beagle")
(define deps '("base" "beagle-lib"))
(define build-deps '("scribble-lib" "racket-doc"))
(define scribblings '(("scribblings/beagle.scrbl" ())))
```

**beagle/info.rkt** — aggregate:
```racket
#lang info
(define collection 'multi)
(define deps '("beagle-lib" "beagle-doc"))
(define build-deps '("beagle-test"))
(define implies '("beagle-lib" "beagle-doc"))
(define version "0.8.0")
```

## What breaks and how to fix it

### Test require paths (9 files)

All 9 test files use relative `"../private/..."` requires. After the move, `tests/` is in `beagle-test/` while `private/` is in `beagle-lib/` — relative paths break. Fix: change to collection-based requires.

```racket
;; Before:  (require "../private/parse.rkt")
;; After:   (require beagle/private/parse)
```

Files to edit (mechanical find-replace in each):
- `tests/parse.rkt` — `parse.rkt`, `types.rkt`
- `tests/check.rkt` — `parse.rkt`, `check.rkt`, `types.rkt`
- `tests/emit.rkt` — `parse.rkt`, `emit.rkt`, `types.rkt`
- `tests/emit-js.rkt` — `parse.rkt`, `check.rkt`, `emit.rkt`, `types.rkt`
- `tests/emit-js-behavioral.rkt` — `parse.rkt`, `check.rkt`, `emit.rkt`, `types.rkt` + runtime-path fix
- `tests/emit-nix.rkt` — `parse.rkt`, `emit.rkt`, `types.rkt`
- `tests/emit-sql.rkt` — `parse.rkt`, `emit.rkt`, `check.rkt`, `types.rkt`
- `tests/lint.rkt` — `parse.rkt`, `lint.rkt`
- `tests/types.rkt` — `types.rkt`

### Runtime path for core.js (1 file)

`tests/emit-js-behavioral.rkt` line 40:
```racket
;; Before:
(define-runtime-path BEAGLE-CORE-JS-PATH "../lib/beagle/core.js")

;; After:
(require setup/collects)
(define BEAGLE-CORE-JS-PATH
  (collection-file-path "lib/beagle/core.js" "beagle"))
```

### flake.nix (line 28)

Currently copies `lang private main.rkt info.rkt` from root. After reorg, source is under `beagle-lib/`:
```nix
# Before:
cp -r lang private main.rkt info.rkt $out/lib/beagle/

# After:
cp -r beagle-lib/lang beagle-lib/private beagle-lib/main.rkt beagle-lib/info.rkt $out/lib/beagle/
cp -r beagle-lib/clj beagle-lib/cljs beagle-lib/js beagle-lib/nix beagle-lib/sql beagle-lib/py $out/lib/beagle/
```

(Note: the current flake already has a bug — doesn't copy target dialect dirs. Fix both.)

### CLAUDE.md / AGENTS.md

Path references like `private/parse.rkt` → `beagle-lib/private/parse.rkt`. Documentation-only change. `bin/` script paths unchanged. Setup instructions change from `raco pkg install --link /home/tom/code/beagle` to the 4-package install.

### Everything that does NOT break

- **`bin/` scripts**: Use collection-based requires (`beagle/private/...`). Unaffected.
- **`raco beagle` command**: Collection-based path in info.rkt. Works.
- **`#lang beagle/*` resolution**: Collection paths preserved by all three sub-packages sharing `collection "beagle"`.
- **`.claude/hooks`**: Reference repo root `/home/tom/code/beagle`, which doesn't change.
- **`examples/`, `experiments/`**: Use `#lang beagle`, resolved via collection. Unaffected.
- **All `private/*.rkt` internal requires**: Sibling-relative paths (`"parse.rkt"`, `"types.rkt"`). The entire `private/` dir moves as a unit.
- **`main.rkt` requires**: Relative `"private/..."`. `main.rkt` and `private/` move together.

## Migration procedure

```bash
# Phase 0: Safety
git add -A && git commit -m "pre-reorg snapshot"
git tag v0.8.0-pre-reorg
find . -type d -name compiled -exec rm -rf {} + 2>/dev/null
raco pkg remove beagle

# Phase 1: Create structure + move files
mkdir -p beagle-lib beagle-test beagle-doc beagle
mv main.rkt lang/ private/ lib/ clj/ cljs/ js/ nix/ sql/ py/  beagle-lib/
mv tests/  beagle-test/
mv scribblings/  beagle-doc/
rm info.rkt   # replaced by 4 new ones

# Phase 2: Write info.rkt files (4 files, contents above)

# Phase 3: Fix test requires (9 files, mechanical replace)

# Phase 4: Fix emit-js-behavioral.rkt runtime-path

# Phase 5: Install packages
raco pkg install --link beagle-lib/ beagle-test/ beagle-doc/ beagle/
raco setup beagle

# Phase 6: Update docs (CLAUDE.md, AGENTS.md, flake.nix)
```

## Verification

```bash
raco pkg show beagle-lib beagle-test beagle-doc beagle     # all 4 installed
raco test beagle-test/tests/                                 # 655+ tests pass
raco scribble --html beagle-doc/scribblings/beagle.scrbl    # docs build
racket -e '(require beagle/private/types) (displayln MAP-TAG)'  # collection resolution
bin/beagle check examples/hello.rkt                          # CLI tools work
raco beagle check examples/hello.rkt                         # raco command works
```

## Rollback

```bash
git checkout v0.8.0-pre-reorg
raco pkg remove beagle beagle-lib beagle-test beagle-doc
raco pkg install --link /home/tom/code/beagle
```

## Critical files

| File | Action |
|------|--------|
| `info.rkt` (root) | Delete |
| `beagle-lib/info.rkt` | Create |
| `beagle-test/info.rkt` | Create |
| `beagle-doc/info.rkt` | Create |
| `beagle/info.rkt` | Create |
| `tests/*.rkt` (9 files) | Change `"../private/..."` → `beagle/private/...` |
| `tests/emit-js-behavioral.rkt` | Fix `define-runtime-path` for core.js |
| `flake.nix` | Update source paths + add missing target dirs |
| `CLAUDE.md` | Update file paths in Architecture section |
| `AGENTS.md` | Update file map paths |
