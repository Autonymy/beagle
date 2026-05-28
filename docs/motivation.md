## Motivation

Today's typed languages have rich type systems but baroque human-optimized
surfaces. Today's dynamic languages have clean surfaces but no type-level
scaffolding. Both flavors evolved before AI code generation existed, and
both made trade-offs that are wrong for models generating code from a spec.

Python is the default model-authored language today by training-data weight;
the model writes it fluently. What the model can't do well in Python is
**lift repeated structure into typed primitives**. Every Django model,
every Pydantic schema, every API client gets hand-written each time because
Python has no macro layer and no rich type system to express the pattern
once. As a domain specializes and the codebase grows, the model's
compression ceiling becomes the bottleneck — not because the model is bad
but because the language doesn't give it the abstractions.

Typed languages with rich macro systems (Common Lisp, Racket, OCaml,
the ML family) hit a different problem: surface sprawl. Five threading
macros means five chances to pick wrong, and the model has no human's
accumulated taste to guide the choice.

Beagle threads the needle: a typed Lisp with **one canonical idiom per
concept**, a curated catalog of typed externs, and rich enough macros to
lift repeated structure — but no more surface than the model actually
needs. The compression ceiling moves up; the hallucination surface stays
low.
