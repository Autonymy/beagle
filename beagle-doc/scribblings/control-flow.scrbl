#lang scribble/manual

@title[#:tag "control-flow"]{Control Flow}

@section[#:tag "if"]{if}

@defform[(if cond then else)]{
Conditional. Type narrows in branches when condition uses @tt{nil?}, @tt{some?}, etc.

@codeblock|{
(if (> x 0) "positive" "non-positive")
}|}

@defform[#:id if-no-else (if cond then)]{
Without else branch, returns @tt{Nil} when condition is false.}

@section[#:tag "cond"]{cond}

@defform[(cond [test body ...] ...)]{
Multi-branch conditional (bracketed style).

@codeblock|{
(cond
  [(< n 0) "negative"]
  [(= n 0) "zero"]
  [(> n 0) "positive"])
}|}

Also supports flat Clojure-style: @tt{(cond test1 body1 test2 body2 :else fallback)}.

@section[#:tag "condp"]{condp}

@defform[(condp pred test value result ... default)]{
Predicate-based dispatch. Tests @tt{(pred value test)} for each clause.
An odd trailing form is the default.

@codeblock|{
(condp = color
  :red   "stop"
  :green "go"
  "unknown")
}|}

@section[#:tag "removed-when-family"]{Removed: when / when-not / if-not / when-let / if-let / when-some / if-some / case}

The 2026-05 surface redesign removed these forms. Migration shapes:

@itemlist[
  @item{@tt{(when c body)} → @tt{(if c body)} or @tt{(if c (do b1 b2 ...))} for multi-body.
        The if-no-else form returns @tt{nil} when condition is false, same as @tt{when} did.}
  @item{@tt{(when-not c body)} → @tt{(if (not c) body)}.}
  @item{@tt{(if-not c t e)} → @tt{(if (not c) t e)}.}
  @item{@tt{(when-let [x v] body)} → @tt{(let [x v] (if x (do body)))}. The eventual typed
        nullable-narrowing form will replace this; it will NOT reuse the @tt{when-let} name.}
  @item{@tt{(if-let [x v] t e)} → @tt{(let [x v] (if x t e))}. Same future-form note as above.}
  @item{@tt{(when-some [x v] body)} / @tt{(if-some [x v] t e)} → same interim shape; superseded
        by future typed-nullable form.}
  @item{@tt{(case x v1 r1 v2 r2 default)} → @tt{(match x [v1 r1] [v2 r2] [_ default])}. The
        case-fold optimization in emit-clj.rkt and emit-rkt.rkt lowers literal-only
        or-patterns to target-native @tt{case} for O(1) dispatch, so the migration ships
        no perf regression.}
]

See @secref{match} for the replacement pattern-dispatch form and the @tt{or}-pattern
extension that absorbs @tt{case}'s literal-dispatch use case.

@section[#:tag "match"]{match}

@defform[(match expr [pattern body ...] ...)]{
Pattern matching with type narrowing.

Patterns:
@itemlist[
  @item{@tt{(RecordName b1 b2 ...)} --- type test + positional field destructuring}
  @item{@tt|{{:key1 pat1 :key2 pat2}}| --- map pattern}
  @item{@tt{nil}, @tt{"str"}, @tt{42} --- literals}
  @item{@tt{(or p1 p2 ...)} --- match any of the sub-patterns (non-binding;
        absorbs the literal-dispatch use case that @tt{case} used to cover)}
  @item{@tt{name} --- bind to variable}
  @item{@tt{_} --- wildcard}
]

@codeblock|{
(defrecord Circle [(radius : Float)])
(defrecord Rect [(width : Float) (height : Float)])

(match shape
  [(Circle r) (* 3.14159 r r)]
  [(Rect w h) (* w h)]
  [_ 0.0])
}|

@codeblock|{
; or-pattern: replaces (case x 1 "one" 2 "two" :else "other")
(match x
  [(or 1 2 3) "small"]
  [(or 4 5 6) "medium"]
  [_ "other"])
}|}

@section[#:tag "try"]{try / catch / finally}

@defform[(try body ... (catch ExType name handler ...) (finally cleanup ...))]{
Exception handling. Multiple @tt{catch} clauses allowed. @tt{finally} is optional.

@codeblock|{
(try
  (Long/parseLong s)
  (catch Exception e
    (println (.getMessage e))
    -1)
  (finally
    (println "done")))
}|}

@section[#:tag "do"]{do}

@defform[(do body ...)]{
Sequences expressions; returns the last value. Used where a single expression
is expected but multiple side effects are needed.

@codeblock|{
(do
  (println "saving...")
  (save-record! rec)
  (println "done")
  rec)
}|}

@section[#:tag "comment"]{comment}

@defform[(comment forms ...)]{
Ignores all forms and returns @tt{nil}. Used for development-time scratch
code and inline examples. The forms are not evaluated or type-checked.

@codeblock|{
(comment
  (start-server 8080)
  (run-tests)
  (println "scratch area"))
}|}
