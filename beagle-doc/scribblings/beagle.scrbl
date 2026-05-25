#lang scribble/manual

@(require (for-label racket/base))

@title{Beagle: An LLM-Optimized Authoring Surface for Dynamic Languages}
@author{Tom Passarelli}

@defmodule[beagle #:lang]

Beagle is what falls out when you redesign a programming surface with a large
language model as the primary author. Today's typed languages have rich type
systems but baroque human-optimized surfaces; today's dynamic languages have
clean surfaces but no type-level scaffolding. Both flavors evolved before AI
code generation existed, and both made trade-offs that were defensible for
humans typing code by hand and are wrong for models generating code from a
spec.

The bet: a model writing code in a typed dynamic-target language with rigorous
compile-time validation, a curated catalog of typed stdlib externs, and one
canonical form per concept will produce more maintainable code at scale than
the same model writing in any existing dynamic language. Where Python (the
default model-authored language by training-data weight) hits a ceiling is
when domains specialize and the language can't compress repeated structure
into typed primitives. Every Django model, every Pydantic schema, every API
client is hand-written each time because Python doesn't give the model a way
to lift the pattern. Beagle's macro layer plus type checking lets the model
author the pattern @italic{once} and have the checker validate every use site.

Beagle's source compiles to multiple targets: Clojure (the original target
and most-developed emitter), ClojureScript, JavaScript, Nix, SQL, Python,
and Typed Racket. The same typed AST drives every emitter; the target is
selected by @tt{#lang} declaration or file extension. Nix has been the most
generative target for the language itself --- working on a non-trivial NixOS
configuration in @tt{#lang beagle/nix} produced design pressure that shaped
@tt{NixType} as an opaque primitive and motivated the schema-driven validator.

@bold{What the checker catches at compile time:}
@itemlist[
  @item{Type mismatches --- passing an @tt{Int} where a @tt{String} is expected}
  @item{Arity errors --- wrong number of arguments to a function}
  @item{Undefined references --- using a name that hasn't been defined}
  @item{Record field errors --- accessing a field that doesn't exist on a record type}
  @item{Cross-module contract violations --- imported function signatures enforced at call sites}
  @item{Refinement violations --- literal values outside declared bounds (e.g., @tt{(->Percentage 150)} when max is 100)}
  @item{Schema violations --- option paths and value types validated against ingested external schemas (NixOS option universe, SQL DDL, with @tt{.d.ts} / OpenAPI / typeshed planned)}
]

@bold{What @secref{surface-overview} covers:} the five design principles
that drove every surface decision, the borrowings from Clojure and Scheme,
the deliberate deviations, the inventions, the drops, and the discipline
that governs further changes. Read it once end-to-end if you want a complete
mental model; the other sections are the form-by-form reference.

@bold{Historical note:} beagle started in early 2026 as "typed Clojure
authoring" --- a thin typed layer over @tt{#lang beagle/clj} emitting plain
Clojure. The multi-target work, the Nix authoring layer, and the explicit
LLM-author framing emerged as the project grew. The Clojure-targeted
emitter is still the most mature; the framing has generalized.

@table-of-contents[]

@include-section["getting-started.scrbl"]
@include-section["surface-overview.scrbl"]
@include-section["forms.scrbl"]
@include-section["types.scrbl"]
@include-section["records.scrbl"]
@include-section["control-flow.scrbl"]
@include-section["iteration.scrbl"]
@include-section["interop.scrbl"]
@include-section["macros.scrbl"]
@include-section["nix-target.scrbl"]
@include-section["tools.scrbl"]
