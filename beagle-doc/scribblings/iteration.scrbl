#lang scribble/manual

@title[#:tag "iteration"]{Iteration and Comprehensions}

Beagle supports Clojure's full iteration toolkit: list comprehensions,
side-effecting loops, counted iteration, and tail-recursive @tt{loop}/@tt{recur}.

@section[#:tag "for"]{for}

@defform[(for [name coll ... :when pred :let [name val ...]] body ...)]{
List comprehension. Binds each name to successive values from its collection.
Optional @tt{:when} clauses filter, @tt{:let} clauses bind intermediate
values. Destructuring works in bindings.

Returns @tt{(Vec BodyType)}.

@codeblock|{
(for [x (range 5) y (range x) :when (even? y)]
  [x y])

;; with :let
(for [item items :let [price (item-price item) tax (* price 0.1)]]
  (+ price tax))

;; with destructuring
(for [[eid name email] contacts]
  (->Contact eid name email))
}|}

@section[#:tag "doseq"]{doseq}

@defform[(doseq [name coll ...] body ...)]{
Side-effecting iteration. Same binding syntax as @tt{for} (multiple bindings,
@tt{:when} and @tt{:let} clauses). Returns @tt{nil}.

@codeblock|{
(doseq [x items :when (pos? x)]
  (println x))
}|}

@section[#:tag "loop"]{loop / recur}

@defform[(loop [name init ...] body ...)]{
Tail-recursive loop. Bindings work like @tt{let}; @tt{recur} jumps back
with new values.

@codeblock|{
(loop [acc 1 n 5]
  (if (<= n 1) acc (recur (* acc n) (- n 1))))
}|}

@section[#:tag "threading"]{Threading: ->>}

@defform[(->> value forms ...)]{
Thread-last: inserts @racket[value] as the last argument of the next form,
then threads that result as the last argument of the form after, and so on.
Used for shaping collection pipelines.

@codeblock|{
(->> items
     (filter even?)
     (map (fn [x] (+ x 1)))
     (reduce +))
}|}

The other threading forms from Clojure (@tt{->}, @tt{cond->}, @tt{cond->>},
@tt{some->}, @tt{some->>}, @tt{as->}) were removed in the 2026-05 surface
redesign:

@itemlist[
  @item{@tt{->} (thread-first) --- use @tt{->>} or a let-chain. Positional
        convenience, not semantic uniqueness.}
  @item{@tt{cond->} / @tt{cond->>} --- use a let-chain with @tt{if} for
        conditional accumulation.}
  @item{@tt{some->} / @tt{some->>} --- use a let-chain with explicit nil-checks.}
  @item{@tt{as->} --- use a @tt{let} with explicit naming for intermediate
        values.}
]

@codeblock|{
; (cond-> order paid? (assoc :status :paid)) becomes:
(let [step1 order
      step2 (if paid? (assoc step1 :status :paid) step1)]
  step2)
}|

@codeblock|{
; (some-> user :address :city) becomes:
(let [a (:address user)]
  (if a (:city a) nil))
}|

@codeblock|{
; (-> person :name (str/upper-case)) becomes:
(str/upper-case (:name person))
; or for longer chains, use ->> with explicit-first-arg via fn:
(->> person ((fn [p] (:name p))) str/upper-case)
}|

The let-chain replacement is verbose at small scale but composes uniformly
with the rest of the surface and produces better error localization than
the threading-macro family.
