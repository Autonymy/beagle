#lang scribble/manual

@title[#:tag "interop"]{Java Interop and Data Literals}

@section[#:tag "java-methods"]{Instance Methods}

@defform[(.method target args ...)]{
Java instance method call. The receiver is typed when declared via
@tt{declare-extern} or the stdlib.

@codeblock|{
(.exists (io/file path))
(.startsWith name "http")
(.trim input)
}|}

@section[#:tag "java-static"]{Static Methods}

@defform[(Class/staticMethod args ...)]{
Java static method call.

@codeblock|{
(System/getProperty "user.home")
(Long/parseLong "42")
(Math/sqrt 2.0)
}|}

@section[#:tag "constructors"]{Constructor Calls}

@defform[(ClassName. args ...)]{
Java constructor. The trailing dot is the marker.

@codeblock|{
(java.io.File. "/tmp/test")
(StringBuilder. "init")
}|}

@section[#:tag "dynamic-vars"]{Dynamic Vars}

Symbols wrapped in @tt{*earmuffs*} are dynamic var references:

@codeblock|{
(first *command-line-args*)
}|

@section[#:tag "with-open"]{with-open}

@defform[(with-open [name expr ...] body ...)]{
Binds resources, evaluates body, then closes all bindings (via
@tt{java.io.Closeable}).

@codeblock|{
(with-open [rdr (clojure.java.io/reader "data.csv")]
  (doall (line-seq rdr)))
}|}

@section[#:tag "doto"]{doto}

@defform[(doto target forms ...)]{
Evaluates @racket[target], threads it as first argument through each form,
returns the original target. Used for Java mutation chains.

@codeblock|{
(doto (java.util.HashMap.)
  (.put "a" 1)
  (.put "b" 2))
}|}

@section[#:tag "import"]{import}

@defform[(import Fully.Qualified.Class)]{
Emits @tt{(:import [package ClassName])} in the generated ns form.

@codeblock|{
(import java.io.File)
(import java.time.Instant)
}|}

@section[#:tag "declare-extern"]{declare-extern}

@defform[(declare-extern name TypeExpr)]{
Declares the type of a function not available via beagle source import.
@bold{Only needed for} Java interop and non-beagle Clojure namespaces.
@bold{Not needed for} cross-module beagle calls.

@codeblock|{
(declare-extern .getAbsolutePath [Any -> String])
(declare-extern System/getProperty [String -> String])
}|}

@section[#:tag "vectors"]{Vector Literals}

@codeblock|{
[1 2 3]
[(->Employee "Alice" 95) (->Employee "Bob" 80)]
}|

Element types are inferred: @tt{[(->Product 1 "A") ...]} gives @tt{(Vec Product)}.

@section[#:tag "maps"]{Map Literals}

@codeblock|{
{:name "Tom" :age 30}
}|

@section[#:tag "sets"]{Set Literals}

@codeblock|{
#{1 2 3}
#{:a :b :c}
}|

@section[#:tag "keywords"]{Keyword-as-Function}

@defform[(:key target)]{
Keyword lookup. If @racket[target] is a typed record, the checker infers
the field type.}

@defform[#:id kw-default (:key target default)]{
Keyword lookup with a default value.

@codeblock|{
(:name person)          ; String if person is typed record
(:age config "unknown") ; with default
}|}

@section[#:tag "regex"]{Regex Literals}

@tt|{#"pattern"}| --- Clojure regex literal, emitted verbatim.

@section[#:tag "metadata"]{Metadata}

@defform[#:id metadata (^ map target)]{
Attaches Clojure metadata to the following form. @tt{^:keyword} is sugar
for @tt{^{:keyword true}}.

@codeblock|{
^{:key (str prefix "-" idx)} [item-view item]
^:private (def internal-state (atom {}))
}|}

@section[#:tag "quote"]{Quote}

Standard Lisp quoting. Quoted forms are not evaluated.

@codeblock|{
'(a b c)    ; quoted list
'foo        ; quoted symbol
}|

@section[#:tag "unsafe"]{unsafe}

@defform[(unsafe "raw-clojure-source")]{
Emits the literal string verbatim into the Clojure output. Works at top-level
and in expression position. Typed as @tt{Any}. Use sparingly --- this is
the escape hatch for Clojure features beagle doesn't cover.

@codeblock|{
(unsafe "(defn helper [x] (some-clj-thing x))")
}|}
