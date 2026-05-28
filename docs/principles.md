## Core Principles

Every surface decision was filtered through these. They are load-bearing.

1. **S-expressions, no compromise on composability.** Uniform
   parenthesized syntax for every construct. No special-case grammar
   per form. Macros, tools, agents all manipulate code as the same
   tree-of-symbols data structure that the parser produces.

2. **Immutability by default; explicit side-effects.** Bindings are
   immutable. Records are functional. There is no implicit aliasing,
   no `set!`, no in-place mutation without an explicit marker. State
   changes go through visible plumbing.

3. **One canonical idiom per concept.** Every concept with N equivalent
   idioms is a 1/N hallucination opportunity. Where two forms claim to
   express the same concept, one gets removed.

4. **Verbose-with-clarity over concise-with-magic.** Explicit positional
   args beat auto-currying. Named bindings beat implicit context.
   Spelled-out forms beat terse aliases.

5. **Failure modes that localize.** When the model writes the wrong
   thing, the error should pinpoint which form and what shape was
   expected.

6. **Zero escape hatches.** No `unsafe-*` anything, no inline target
   passthrough, no verbatim-string-to-target forms under any name.
   Every gap closes by adding a stdlib entry, adding a typed surface
   form, or writing a sibling target-language file and importing it.
   The filesystem boundary is auditable; an inline backdoor is not.

7. **Consistency compounds; ergonomic savings don't.** A form earns its
   place by reinforcing a pattern that shows up elsewhere. Forms that
   exist for local character savings, with no broader pattern, are
   net-negative.
