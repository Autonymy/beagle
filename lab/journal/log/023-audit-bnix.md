● Now the deep analysis. 25 tasks queued. Let me give you the
  full picture.

  ---
  bnix surface area — deep audit

  Scope reviewed: emit-nix.rkt (1071L), validate-nix.rkt
  (796L), nixos-schema.rkt (288L), stdlib-nix.rkt (136L),
  rename-nix.rkt (223L), nix AST structs (ast.rkt:212-227),
  reader (nix/lang/reader-impl.rkt), parse cases
  (parse.rkt:911-1022), all 9 .bnix fixtures, 8 round-trip
  tests.

  What's architecturally right (and stays right)

  dimension: pipeline
  observation: parse → check → emit, same AST as every other
    target. Zero parallel universe.
  ────────────────────────────────────────
  dimension: reader
  observation: minimal: just adds ''...'' for Nix's indented
    strings via readtable extension, preserves provenance via
    #%nix-string tag with source location. No reader DSL.
    (reader-impl.rkt:75-92)
  ────────────────────────────────────────
  dimension: reuse
  observation: let, if, cond, defn, defrecord, defenum,
    deferror, threading, match — all just work. ~50
    cross-target forms ride free.
  ────────────────────────────────────────
  dimension: fn emission
  observation: curried lambdas (a: b: body) — matches
    Nix-native semantics, not "currying via array trick" or
    other hacks. (emit-nix.rkt:135-148)
  ────────────────────────────────────────
  dimension: attrset paths
  observation: {:foo.bar 1} flattens to native foo.bar = 1;
  via
    flatten-dot-path. Output reads like hand-written Nix, not
    transpiled blob. (:765-799)
  ────────────────────────────────────────
  dimension: qualified call convention
  observation: lib/mkIf → lib.mkIf, borrows Clojure's /
    semantics. Single rule. (:667-673)
  ────────────────────────────────────────
  dimension: record emission
  observation: constructor ->Foo + per-field accessors
    foo-fieldname. Matches beagle convention exactly.
    (:209-244)
  ────────────────────────────────────────
  dimension: separate schema validator
  observation: validate-nix.rkt does mkOption validation,
    duplicate detection, cross-file conflict detection,
    Levenshtein-driven did-you-mean, auto-fix mode — separate
    from type check, runs on source for line precision.
    Sophisticated.
  ────────────────────────────────────────
  dimension: reserved-word handling
  observation: colliding identifiers get ' appended.
    Nix-correct. (:28)
  ────────────────────────────────────────
  dimension: renamer is text-based with quote-awareness
  observation: dotted paths get word-boundary matching;
  matches
    inside string literals are skipped.
  (rename-nix.rkt:89-102)
  ────────────────────────────────────────
  dimension: three string strategies
  observation: s inline-interp, ms multiline-interp, ''...''
    reader-native — covers the design space coherently.

  The bones are good. The pipeline is right. Now the surgery.

  Sharp edges — concrete defects

  Tier 1: actual bugs (silent corruption / broken forms)

  #: B1
  location: emit-nix.rkt:695
  bug: (mod a b) emits (a /* mod */ b) — literal C-style
    comment as an infix operator. Syntactically broken Nix
    output.
  ────────────────────────────────────────
  #: B2
  location: emit-nix.rkt:349-351, 905-922
  bug: recur-form always emits null /* recur outside loop */.
    emit-loop wraps body in a lambda but never substitutes
    recur calls. Every (loop ...) with recur is broken.
  ────────────────────────────────────────
  #: B3
  location: emit-nix.rkt:355-360 vs :226, 855
  bug: check-expr/rescue-form read __tag (double underscore);
    defrecord writes _tag (single); pat-record reads _tag.
    (check (some-result)) will never match a defrecord-derived

    Ok.
  ────────────────────────────────────────
  #: B4
  location: emit-nix.rkt:494
  bug: Generic else emits null /* unsupported: ~v */. Silent
    output corruption. Every other target errors.
  ────────────────────────────────────────
  #: B5
  location: emit-nix.rkt:317-319
  bug: set-form silently emitted as list. Two distinct types
    collapse.
  ────────────────────────────────────────
  #: B6
  location: emit-nix.rkt:386-392
  bug: method-call? silently emits target.method args — Nix
  has
    no methods, this invents syntax.
  ────────────────────────────────────────
  #: B7
  location: emit-nix.rkt:368-370
  bug: try-form emits builtins.tryEval (body), which returns
    {success, value}, not the inner value. Downstream code
  that
     expects the value gets a wrapper struct.
  ────────────────────────────────────────
  #: B8
  location: emit-nix.rkt:879-901
  bug: for honors only first binding + first :when. Cartesian
    product, :let, :while silently dropped.
  ────────────────────────────────────────
  #: B9
  location: emit-nix.rkt:1018-1056
  bug: extract-cfg-root hardcodes regex
    options\.myConfig\.modules\.NAME then
  substring-substitutes
     cfg. into the already-emitted  output string. (a)
    user-specific firn/nixos-config sugar  leaking into the
    compiler; (b) substring rewrite breaks  any literal
    occurrence inside a string. This is the  worst code in the

    file.
  ────────────────────────────────────────
  #: B10
  location: stdlib-nix.rkt:122-136
  bug: lib/types.bool, lib/types.int, etc. all typed as Any.
    You can pass a Bool literal where a NixType is expected
  and
     the checker won't complain. The whole module-options
    surface area is type-Swiss-cheese.

  Tier 2: surface inconsistencies (design noise)

  The Nix-specific cluster has 18 surface forms using four
  naming strategies:

  ┌───────────────────┬───────────────────────────────────┐
  │     strategy      │               forms               │
  ├───────────────────┼───────────────────────────────────┤
  │ abbreviation      │ inh, inh-from, rec-att, spath, s, │
  │                   │  ms, p                            │
  ├───────────────────┼───────────────────────────────────┤
  │ -do suffix        │                                   │
  │ (collision        │ with-do, assert-do                │
  │ avoidance)        │                                   │
  ├───────────────────┼───────────────────────────────────┤
  │                   │ module (= fn-set-rest exactly,    │
  │ sugar alias       │ same struct, same fields —        │
  │                   │ parse.rkt:992-1000)               │
  ├───────────────────┼───────────────────────────────────┤
  │                   │ fn-set, fn-set-rest, fn-set@,     │
  │ descriptive       │ get-or, has, impl, pipe-to,       │
  │                   │ pipe-from                         │
  └───────────────────┴───────────────────────────────────┘

  Specific frictions:

  - with-do vs with-form — beagle's record-update with lives
  next to nix's with-do (scope). Confusable; the -do suffix is
   purely defensive. (emit-nix.rkt:339 and :427)
  - impl — collides with the protocol-implementation impl
  keyword used in parse-impl-method / type-impl
  (parse.rkt:1791-1810). Cross-target shadowing of a
  definition-form name onto a logical operator.
  - module — pure alias for fn-set-rest. Violates "one
  canonical idiom per concept" (the CLAUDE.md rule).
  - inh vs defrecord — the codebase elsewhere doesn't
  abbreviate (defrecord, not defr). Why does inherit get
  crushed to 3 letters?
  - s / ms / str — three string ops side by side. str is
  concat (cross-target), s is single-line interp, ms is
  multi-line interp. None feel like the others.
  - pipe-to / pipe-from — same concept as -> / ->> threading
  already in beagle. Asymmetric naming.

  Tier 3: missing capabilities (surface gaps)

  - No derivation / mkDerivation form. The single most
  important Nix idiom has zero syntactic affordance.
  - No flake form. Flakes are the primary Nix authoring
  surface in 2026.
  - No overlay [final prev] {...} sugar. Hand-written via
  fn-set-rest.
  - No import shorthand. (builtins/import path) works but
  lacks the import path args flavor.
  - No reader-level ~"...${x}..." string interp. Forces (s
  ...) for what should be syntactic.
  - defscalar is a comment on nix target
  (emit-nix.rkt:185-188). Newtype branding lost.
  - Stdlib gaps: lib.versionAtLeast is used in nix-kmod.bnix
  fixture but isn't even typed (only lib.versionAtLeast shows
  up via the qualified-call fallback). 104 entries vs the
  ~1000+ surface area of nixpkgs.lib and builtins.
  - No NixOS-module skeleton form. (nixos-module {options ...
  config ... imports ...}) could validate the canonical
  three-key shape.
  - No nix-aware lint passes. mkIf false, redundant mkDefault,
   etc. all silently ship.
  - No Scribble docs for beagle/nix in
  beagle-doc/scribblings/.

  Tier 4: code quality

  - emit-nix.rkt is 1071 lines, one file. Compare to the other
   emit targets which are similar size but split. Mixes
  program emission, expression dispatch, the stdlib
  translation table (~30 special-cased call patterns),
  nix-specific helpers, and the magical cfg-rewriter.
  - Four nearly-identical string-escape functions
  (escape-nix-string, escape-nix-string-keep-interp,
  escape-nix-multiline, escape-nix-multiline-keep-interp) —
  each a triple-nested regexp-replace*. Could be one fn with
  two flags.
  - HOME-MANAGER-ROOTS and FREEFORM-KEY-PREFIXES hardcoded in
  validate-nix.rkt:100,135 — user-specific config bleeding
  into the compiler. Same problem as B9.
  - paren-wrap (:698) has special-cases for nix-ident and
  infix calls but doesn't know about nix-fn-set or nix-impl.
  Edge-case parens are ad hoc.
  - Test coverage: ~8 round-trip tests for nix vs ~1300 total.
   Undertested.

  The todo list (25 tasks queued)

  Tiered by ROI:

  Bugs (fix first): #1 mod, #2 loop/recur, #3 _tag, #4 silent
  fallback, #5 set/method-call, #6 try semantics, #7 for-form,
   #8 myConfig regex magic, #9 NixType opaque type, #25
  defscalar.

  Naming polish (design coherence): #10 unify naming, #11
  with-do/with collision, #12 module alias, #13 impl
  collision, #23 reader-level interp.

  Capability gaps: #14 derivation form, #15 flake form, #16
  overlay form, #17 stdlib expansion 104 → ~300.

  Codebase quality: #18 Scribble docs, #19 test expansion, #20
   split emit-nix.rkt, #21 unify escapes, #22 externalize
  validator config, #24 nix-aware lints.

  If you do just #2, #3, #6, #8, #9, #11, #12 —
  silent-corruption bugs and the two worst naming collisions
  go away. That's the floor for "well-designed." For "tears of
   joy," #14, #15, and #23 are the highest-leverage capability
   adds — derivations + flakes + reader-interp would let you
  write 80% of real-world Nix in idiomatic bnix.

