## Why compute-by-default? And why the colons?

Beagle evaluates by default: a form runs unless marked inert. Keys are keywords (`:enable`), because in an evaluated container a bare symbol is a reference, so literal keys carry the colon. This is not an ergonomic preference — it is the load-bearing decision the whole analysis story rests on.

The alternative is **data-by-default**: forms are inert, and you opt into computation. That model existed at the center of Lisp through the 1970s — fexprs, in MacLisp and Interlisp — and was deliberately abandoned after Pitman's 1980 *Special Forms in Lisp*.

The reason: with data-by-default, static analysis cannot generally tell whether a form will evaluate or stay inert, so the compiler cannot safely analyze, optimize, or check a subtree. Compute-by-default plus a syntactic inert marker — `quote` — keeps evaluation statically decidable. You can see from the source what runs.

Beagle's value over a schema validator like nisp is precisely its analysis layer:

* typed AST
* confidence-ranked repair
* oracle-verified fixes
* static refinement checks

That layer requires a statically analyzable surface. Compute-by-default is what makes the surface analyzable. The colons are the visible cost of the property that buys the entire repair pipeline. They stay because the analysis stays — same decision, two faces.

Inert data is available via quote: `'(…)`. That is a static marker, not a runtime choice.

Frozen maps/vectors may be added the same way if a use case warrants — always as syntax, never as runtime-decided inertness.
