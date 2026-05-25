#lang scribble/manual

@title[#:tag "surface-overview"]{Surface Overview}

This document is the executive summary of beagle's surface: what it
is, what it borrowed, what it invented, what it deliberately rejected,
and what's still open. It is meant to be read end-to-end in one
sitting and give you a complete-enough mental model that you can
write or read beagle code without consulting other references.

Companion documents go deeper on individual topics: @secref{forms} for
the form catalog, @secref{types} for the type system,
@secref{control-flow} for conditionals and bindings,
@secref{records} for data definitions, @secref{macros} for the
macro system. This overview is the map; those are the territory.

@section[#:tag "what-beagle-is"]{What beagle is}

Beagle is a typed authoring layer with one source language and
multiple target backends. You write @tt{.bgl} (or target-specific
sibling extensions: @tt{.bclj}, @tt{.bcljs}, @tt{.bjs}, @tt{.bnix},
@tt{.bsql}, @tt{.bpy}, @tt{.brkt}) and the compiler emits Clojure,
ClojureScript, JavaScript, Nix, SQL, Python, or Typed Racket source
for runtime. The same typed AST drives every emitter.

The primary user of beagle is an LLM generating beagle code from a
spec or repair task. Every design decision is filtered through this
lens: a human Clojure developer with muscle memory will find some
choices odd; a Scheme purist will find others odd; neither audience
is the target. The target is a model picking forms by probability over
training data, and the surface is optimized to minimize the
probability that the model picks wrong.

Runtime is whatever the target language provides. Beagle does not
ship a runtime; it ships a compiler and a typed stdlib catalog. The
compiler is currently written in Racket; self-hosting (compiler
written in beagle, compiled to Cyclone Scheme) is the next major
work.

@section[#:tag "philosophy"]{The five principles}

@bold{1. One canonical idiom per concept.} Every concept with N
equivalent idioms is a 1/N hallucination opportunity at generation
time. Five threading macros isn't ergonomics --- it's five chances to
pick wrong. Where two forms claim to express the same concept, one
gets removed; where they express genuinely different concepts, both
stay and the documentation makes the distinction explicit.

@bold{2. Verbose-with-clarity over concise-with-magic.} Explicit
positional args beat auto-currying. Named bindings beat implicit
context. Spelled-out forms beat terse aliases. Generation cost is
amortized to the model; ambiguity cost compounds at every read site.
Beagle is not optimized for keystrokes typed by humans.

@bold{3. Failure modes that localize.} When the model writes the
wrong thing, the error should pinpoint which form and what shape was
expected. Forms whose shape matches what the type system understands
produce better errors. A form that errors with "expected one of A, B,
C; got X; did you mean A?" is worth more than a form that errors with
"undefined behavior."

@bold{4. No escape hatches.} There is no @tt{unsafe-js},
@tt{unsafe-clj}, @tt{unsafe-nix}, no inline target-language passthrough,
no @tt{(define-macro unsafe ...)}. Every gap is closed by either (a)
adding a stdlib type signature, (b) adding a surface form, or (c)
writing a sibling target-language file and importing it. The
filesystem boundary between beagle source and target-language
source is auditable; an inline backdoor is not. Every typed
language that shipped an escape hatch regretted it; beagle refuses
the shortcut.

@bold{5. Consistency compounds; ergonomic savings don't.} A form
earns its place by reinforcing a pattern that shows up elsewhere in
beagle. Forms that exist for local character savings, with no broader
pattern they reinforce, are net-negative even when they look
convenient at authoring time. The test for every form: does this make
the rest of the surface more predictable, or is it a separate fact to
memorize?

@section[#:tag "from-clojure"]{What beagle took from Clojure}

The shapes that survived the audit because they're universal Lisp
idioms or Clojure innovations worth keeping:

@itemlist[
  @item{@bold{S-expressions and @tt{[]} vs @tt{()} distinction.}
        Vectors are syntactically distinct from lists; the reader
        preserves the difference. This is Clojure's contribution to
        Lisp ergonomics and it earns its place.}
  @item{@bold{@tt{(ns ...)} for namespace declaration.} Universal
        Clojure idiom, heavily represented in LLM training data,
        unambiguous.}
  @item{@bold{Records, protocols, and @tt{extend-type}.} The data /
        behavior separation Clojure landed on is cleaner than
        single-rooted OO and survives the typing layer. @tt{deftype}
        was dropped because it bundled @tt{defrecord} + protocol-impl
        into one form, but the two-form decomposition is kept.}
  @item{@bold{Tagged unions via @tt{defunion}}, in the spirit of
        Clojure's @tt{defrecord}+protocol pattern but typed.}
  @item{@bold{Pattern matching via @tt{match}}, similar shape to
        core.match. Extended with @tt{or}-patterns to absorb
        @tt{case}.}
  @item{@bold{Map literals @tt{{}}, set literals @tt{#{}}, keyword
        access via @tt{get}.} The data-literal syntax is too good not
        to take.}
  @item{@bold{Threading via @tt{->>}.} One threading form survived
        the audit. @tt{->} was dropped (positional convenience, not
        semantic uniqueness); the @tt{cond->/as->/some->} family was
        dropped (compositions of conditional + threading, expressible
        as let-chains).}
  @item{@bold{Common stdlib names:} @tt{first}, @tt{rest},
        @tt{count}, @tt{empty?}, @tt{conj}, @tt{assoc},
        @tt{dissoc}, @tt{map}, @tt{filter}, @tt{reduce}, etc.
        Clojure's names are more readable than Scheme's
        @tt{car}/@tt{cdr}/@tt{length}/@tt{null?} and the typing layer
        doesn't change that.}
  @item{@bold{@tt{loop}/@tt{recur} for tail-recursive iteration.}
        Survived the audit on reflex evidence --- the canonical signal
        is that the agent reaches for it without prompting.}
]

@section[#:tag "from-scheme"]{What beagle took from Scheme and Racket}

@itemlist[
  @item{@bold{Custom @tt{#lang} via Racket's reader-extension
        machinery.} The whole compiler is a Racket reader/expander
        pipeline that intercepts the source, parses it into beagle's
        typed AST, and emits target source. This is the
        infrastructure that made beagle implementable in a weekend
        rather than a year.}
  @item{@bold{Hygienic macro thinking.} Template macros preserve
        binding scope correctly; capture-avoiding by construction.
        Beagle's template macros are simpler than Racket's
        @tt{syntax-rules}/@tt{syntax-case} because the only kind is
        @tt{safe} (type-checked end-to-end), but the hygiene
        guarantees come from the same lineage.}
  @item{@bold{Procedural macros via @tt{define-macro proc}.} Macros
        whose bodies are evaluated by the host Racket at compile
        time, with typed AST contracts on input and output.}
  @item{@bold{Typed Racket as oracle target.} @tt{beagle/rkt} emits
        Typed Racket source. @tt{raco make} on the output
        independently validates that beagle's type promises are
        sound under Typed Racket's checker --- a second opinion that
        catches inference bugs the beagle checker misses.}
]

What beagle deliberately did NOT take from Scheme:

@itemlist[
  @item{@tt{car}/@tt{cdr}/@tt{cons}/@tt{null?} names --- replaced
        with Clojure equivalents.}
  @item{@tt{(define (name args) body)} function-definition shorthand
        --- replaced with @tt{defn} / @tt{def}.}
  @item{Continuations and dynamic-wind --- not modeled in the type
        system; target backends don't all support them.}
]

@section[#:tag "deviations"]{What beagle deviated from both traditions on}

@bold{Wrapped type annotations: @tt{(name : Type)} not @tt{name :- Type}.}
A 6-variant benchmark showed no measured benefit from inline-marker
variants. The wrapped form has unambiguous parse with no lookahead,
which matters for both the parser and the model generating it.

@bold{Wrapped params only: @tt{(defn f [(x : Int) (y : Int)] ...)}.}
Inline-typed params (@tt{(defn f [x : Int y : Int] ...)}) were tried
and removed --- ambiguous parse, no measured benefit, less LLM-friendly.

@bold{Strict mode is the default.} Dynamic mode exists as a per-file
opt-out for humans who want to prototype without types. The compiler
defaults to strict because the model should always stay strict;
relaxed mode is human-comfort scaffolding.

@bold{P2 checker profile is the default.} Empirically chosen from
the E16 experiments: P2 (exhaustive match warnings + flow-sensitive
narrowing) is the sweet spot for agent-assisted development. P3
effects add no measured value; P1 false positives actively hurt by
making the agent rewrite working code.

@bold{Multi-target IR, not a Clojure transpiler.} The same typed AST
emits to Clojure, ClojureScript, JavaScript, Nix, SQL, Python, or
Typed Racket. Target is selected by @tt{#lang} declaration. The Nix
emitter is especially load-bearing --- beagle is the authoring layer
for the maintainer's NixOS configuration, where Nix's untyped DSL
is a regular source of bugs that beagle catches at compile time.

@bold{No user-defined type aliases.} @tt{Number} is the only built-in
alias (@tt{(U Int Float)}). Users are pushed to write @tt{Int} or
@tt{Float} concretely when the concrete type is known. Aliases were
considered and rejected --- they introduce a layer of indirection
between what the user wrote and what the checker reads, which makes
errors less localized.

@section[#:tag "invented"]{What beagle invented (or assembled new)}

@bold{Parametric defunion with throwable variants:}
@codeblock|{
(defunion :throwable AuthError
  (Forbidden  [(reason : String)])
  (Expired    [(at : Int)])
  (Malformed  [(input : String)]))
}|
Both data and error types use the same shape. Throwables are a
keyword annotation, not a separate form. Replaced the old
@tt{deferror}.

@bold{Refinement predicates on scalars:}
@codeblock|{
(defscalar Percentage Int :where (between 0 100))
}|
Compile-time check on literal values: @tt{(->Percentage 150)} errors
at parse time. Catches bugs the type system alone misses.

@bold{The bracket-clause family.} A shape that recurs across
@tt{let}, @tt{loop}, @tt{cond}, @tt{match}, @tt{for}, @tt{doseq},
@tt{defrecord}, and others: @tt{[item item ...]} inside the form,
where each item is either a pair or a structured sub-form. Once you
internalize the pattern, every form that uses it is immediately
readable. This is one of the strongest pattern-extending wins in the
surface --- a new form that uses bracket-clauses costs zero
mental overhead.

@bold{Stdlib extern catalog with typed signatures.} Roughly 1800
pre-typed entries across portable + per-target catalogs. Calls into
target-language functions (Clojure's @tt{clojure.string/upper-case},
JavaScript's @tt{Array.prototype.includes}, Nix's @tt{builtins.fetchurl},
etc.) get full type checking at the call site. This is the single
biggest leverage point for AI type-safety --- the agent can't
hallucinate a wrong-arity call to @tt{(reduce ...)} because the
checker knows reduce's signature.

@bold{Reactive daemon with AST cache.} @tt{bin/beagle-daemon}
provides ~100ms re-checks via mtime-invalidated AST caching. 45×
faster query lookups, ~0.6s warm builds vs ~3s cold. Built because
agent-assisted development requires sub-second feedback loops; without
the daemon, the agent spends most of its time waiting on cold compiles.

@bold{Self-host-as-validation.} @tt{self-host/} contains beagle's
compiler written in beagle (target: JavaScript via @tt{.bjs}). It's
not yet the production compiler --- the Racket implementation still
ships --- but the parity check (11/11 emission tests passing)
proves the surface is expressive enough to compile itself, which is
the load-bearing precondition for the upcoming Cyclone self-host.

@section[#:tag "rejected"]{What beagle deliberately rejected}

These were considered and rejected because they failed the
predictability or no-escape-hatches tests:

@itemlist[
  @item{@bold{@tt{#(...)} anonymous fn shorthand.} Alternate idiom
        for @tt{fn}, no clarity gain, just another shape to recognize.}
  @item{@bold{@tt{@"@"deref}, @tt{#'var-quote}.} Clojure-runtime
        concepts that don't translate cleanly across all targets.}
  @item{@bold{Exotic reader macros (@tt{#=}, @tt{#_}, @tt{#?}).}
        Clojure-reader-specific, complicate the parse, no broad pattern.}
  @item{@bold{Inline target-language strings.} No
        @tt{(unsafe-js "raw javascript here")}, no
        @tt{(unsafe-nix "literal nix")}. If you need raw target code,
        write a sibling file in the target language and import it ---
        the filesystem boundary is auditable.}
  @item{@bold{User-defined type aliases.} Introduces indirection
        that worsens error localization.}
  @item{@bold{@tt{any} / dynamic typing as an escape hatch.}
        Strict mode is the default and there's no per-expression
        opt-out. @tt{Any} the type exists for genuine top-of-lattice
        cases, not as a "trust me" bailout.}
]

@section[#:tag "dropped"]{Surface forms dropped during the 2026-05 audit}

For posterity, and so future-instances don't propose adding any back:

@itemlist[
  @item{@tt{defmulti} / @tt{defmethod} --- replace with
        @tt{defprotocol} + @tt{extend-type}. Zero corpus usage;
        protocol dispatch covers the cases.}
  @item{@tt{deftype} --- replace with @tt{defrecord} +
        @tt{extend-type}. Bundled two concepts (data shape +
        protocol attachment) into one form.}
  @item{@tt{->} --- replace with @tt{->>} or a let-chain. Positional
        convenience, not semantic uniqueness.}
  @item{@tt{as->}, @tt{cond->}, @tt{cond->>}, @tt{some->},
        @tt{some->>} --- replace with let-chains using @tt{if} or
        explicit nil-checks. Compositions of threading + conditional;
        zero corpus usage.}
  @item{@tt{when} --- replace with @tt{(if c body)} for single body
        or @tt{(if c (do b1 b2 ...))} for multi-body. Sugar over if +
        do.}
  @item{@tt{when-not} / @tt{if-not} --- replace with
        @tt{(if (not c) ...)}. Sugar.}
  @item{@tt{when-let} / @tt{if-let} --- replace with
        @tt{(let [x v] (if x ...))}. Clojure truthy-binding semantics
        should not be inherited by the future typed-nullable form.}
  @item{@tt{when-some} / @tt{if-some} --- same replacement as above.
        Truthy-vs-nil distinction superseded by future typed-nullable
        design.}
  @item{@tt{dotimes} --- replace with @tt{(doseq [i (range n)] ...)}.
        Sugar over doseq + range.}
  @item{@tt{case} --- replace with @tt{match} using or-patterns.
        Folded into match; case-fold optimization preserves perf at
        the emit layer.}
  @item{@tt{(:keyword target)} --- replace with @tt{(field r)} for
        records, @tt{(get m :key)} for maps. Overloaded one shape
        for two distinct operations.}
  @item{@tt{inc} / @tt{dec} --- replace with @tt{(+ x 1)} /
        @tt{(- x 1)}. Sugar.}
  @item{@tt{not=} --- replace with @tt{(not (= a b))}. Sugar.}
  @item{@tt{deferror} --- replace with
        @tt{(defunion :throwable Name ...)}. Unified into defunion
        with throwable keyword.}
  @item{@tt{(define-macro unsafe ...)} --- replace with
        @tt{(define-macro safe ...)}; template macros are now
        type-checked end-to-end and the unsafe kind has been removed
        as an escape hatch on the macro shape.}
  @item{@tt{(unsafe ...)} / @tt{(unsafe-js ...)} etc. inline target
        passthrough --- add a stdlib signature for the missing
        function, or write a sibling target-language file and import
        it. Inline target-language passthrough is an escape hatch.}
]

@section[#:tag "kept-after-audit"]{Surface forms kept after audit (where you might expect a drop)}

Each of these looked like a drop candidate but survived empirical
examination. Documented here so future-instances don't re-audit:

@itemlist[
  @item{@bold{@tt{loop} / @tt{recur}.} Reflex signal: the agent
        reaches for it without prompting when writing tail-recursive
        algorithms. Beagle's @tt{let} doesn't support named-let,
        so there's no actual alternative. Dropping would have
        @italic{added} idiom count, not reduced it.}
  @item{@bold{@tt{->Name} constructor.} Looks redundant with a bare
        @tt{(Name ...)} call but the bare form doesn't exist in
        beagle. No alternative to redirect to.}
  @item{@bold{@tt{->>}.} 1 corpus usage at audit time, looks droppable.
        But it sits alone in its concept space (threading), is
        canonical Clojure-trained-data, and removing it would make
        threading require manual let-chains for every case. Low
        usage reflects let-chain-heavy style, not redundancy.}
  @item{@bold{@tt{cond}.} Looks like @tt{match} could absorb it.
        It can't: @tt{cond} is sequential independent-predicate
        dispatch, @tt{match} is pattern-against-target dispatch.
        Forcing cond into match requires a synthetic target and
        ugly bindings. Two distinct concepts.}
  @item{@bold{@tt{do}.} Survived re-examination after @tt{when}
        dropped. Multi-expression sequencing is genuinely useful
        for side-effect blocks inside @tt{if} branches and similar.}
  @item{@bold{@tt{nth} vs @tt{get}.} Two forms, two concepts. @tt{nth}
        is positional-int into vector; @tt{get} is keyed lookup
        on map. Same predictability test that kept @tt{cond} vs
        @tt{match}.}
  @item{@bold{@tt{for} vs @tt{doseq} vs @tt{map}/@tt{filter}/@tt{reduce}.}
        Three concepts: collection comprehension that yields a value
        vs side-effect iteration that returns nil vs higher-order
        function pipeline. Not redundancy.}
  @item{@bold{Three record-access mechanisms.} @tt{(field r)} for
        record auto-accessors, @tt{(get m :key)} for map lookup,
        @tt{(.-field obj)} for JS interop property access. Three
        concepts, three forms. The @tt{(:keyword target)} drop
        eliminated the actual overload.}
]

@section[#:tag "open"]{What's still open}

Two questions remain unresolved. Both are deferred to external
triggers rather than open audit threads:

@bold{Nil semantics and typed nullable-narrowing.} Beagle currently
uses @tt{(let [x v] (if x then else))} as the interim pattern for
nil-safe binding. The eventual typed form will absorb this with a
nullable-narrowing primitive whose exact name and shape depends on
broader nil-semantics decisions still to be made. The
@tt{when-let}/@tt{if-let} drop included an explicit instruction:
the typed form should NOT reuse those names. They carry Clojure
truthy-binding semantics; the typed form should be beagle-native.

@bold{Macro DSL audit.} Three kinds currently: @tt{safe} (template,
type-checked end-to-end), @tt{proc} (Racket-evaluated procedural with
typed AST contracts), @tt{beagle} (beagle-evaluated procedural,
dormant with 0 corpus usage but retained for the Cyclone-self-host
case). The final audit is blocked on Cyclone self-host because
Cyclone's metaprogramming capacity shapes the answer --- @tt{proc}
needs Racket at compile time, and under Cyclone that mechanism is
either ported, replaced, or restricted to a Cyclone-evaluable subset.

@section[#:tag "closed"]{What's closed}

Surface-redesign-as-dominant-mode ended 2026-05-25. The audit cycle
reached its natural endpoint per the four criteria documented in
@filepath{lab/journal/synthesis/design-principle.md}. Drop candidates
from here forward must come from concrete friction in real use, not
from another pass through the form list.

This is a closure of @italic{mode}, not of question set. The two
open questions above will get answered when their dependencies land
(nil-semantics: when the design work happens; macro-DSL: when Cyclone
gives the answer concrete constraints). Until then, the surface is
fit-for-use as-is.

The current discipline: surface changes are responsive to use, not
to perfection. The audit produced a tight, predictable surface; the
remaining work is using it.

@section[#:tag "where-to-go-next"]{Where to read next}

@itemlist[
  @item{@secref{forms} --- complete catalog of surface forms with
        examples.}
  @item{@secref{types} --- the type system, primitives, parametric
        types, unions, refinement.}
  @item{@secref{records} --- @tt{defrecord}, @tt{defunion}, @tt{defscalar},
        @tt{defenum}, @tt{defprotocol}, @tt{extend-type}.}
  @item{@secref{control-flow} --- @tt{if}, @tt{cond}, @tt{match},
        @tt{let}, @tt{loop}/@tt{recur}.}
  @item{@secref{macros} --- @tt{define-macro safe} and
        @tt{define-macro proc}; how expansion is type-checked.}
  @item{@filepath{lab/journal/synthesis/design-principle.md} ---
        the long-form philosophy with full rationale for each
        decision and the audit-endpoint discipline.}
  @item{@filepath{lab/journal/log/} --- chronological audit notes
        for individual drops, especially logs 024-027 for the
        2026-05 redesign cycle.}
]
