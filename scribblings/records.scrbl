#lang scribble/manual

@title[#:tag "records"]{Records, Scalars, and Enums}

@section[#:tag "defrecord"]{defrecord}

@defform[(defrecord Name [(field : Type) ...])]{
Defines a typed record. Generates a constructor, typed accessors, and
keyword-access support.

@codeblock|{
(defrecord Employee [(name : String) (rate : Long)])

(def alice (->Employee "Alice" 95))
(def n : String (employee-name alice))
(:name alice)  ; returns String via keyword inference
}|

Generated functions:
@itemlist[
  @item{Constructor: @tt{->Employee} typed @tt{[String Long -> Employee]}}
  @item{Accessors: @tt{employee-name} typed @tt{[Employee -> String]},
        @tt{employee-rate} typed @tt{[Employee -> Long]}}
  @item{Keyword access: @tt{(:name e)} infers field type when @tt{e} is
        a known @tt{Employee}}
]

Compiles to Clojure @tt{defrecord} plus generated accessor functions:
@codeblock|{
;; Generated Clojure:
(defrecord Employee [name rate])
(defn ->Employee [name rate] (Employee. name rate))
(defn employee-name [r] (:name r))
(defn employee-rate [r] (:rate r))
}|}

@section[#:tag "with"]{with (record update)}

@defform[(with record [:field value] ...)]{
Typed record update. Compiles to @tt{(assoc record :field1 val1 ...)}.
The type checker verifies each field exists on the record type and
the value type matches the field's declared type.

@codeblock|{
(defrecord Order [(status : String) (total : Long)])
(defn confirm [(o : Order)] : Order
  (with o [:status "confirmed"]))
}|}

@section[#:tag "defscalar"]{defscalar (nominal types)}

@defform[(defscalar Name BackingType)]{
Creates a nominal type wrapping a primitive. @tt{Amount}, @tt{Timestamp},
and @tt{AccountId} can all be @tt{Long} at runtime but the type checker
treats them as incompatible.

@codeblock|{
(defscalar Amount Long)
(defscalar Email String)

(def total (->Amount 5000))     ; wrap
(def n (amount-value total))    ; unwrap: Long
}|}

@defform[#:id defscalar-refined (defscalar Name BackingType :where (pred) ...)]{
Refinement predicates add compile-time literal checking and runtime @tt{:pre}
conditions.

@codeblock|{
(defscalar Percentage Long :where (>= 0) (<= 100))
(->Percentage 150)   ; compile-time error: 150 violates (<= 100)
}|}

@section[#:tag "defenum"]{defenum}

@defform[(defenum Name :value ...)]{
Declares an enum value set. Compiles to @tt{(def Name-values #{:value1 ...})}.
Useful for constraining keyword fields to a known set of values.

@codeblock|{
(defenum OrderStatus :placed :confirmed :paid :shipped :delivered :cancelled)
}|}

@section[#:tag "defprotocol"]{defprotocol}

@defform[(defprotocol Name (method-name [params] : ReturnType) ...)]{
Defines a protocol with typed method signatures.

@codeblock|{
(defprotocol Greetable
  (greet [(self : Any)] : String))
}|}

@section[#:tag "deftype"]{deftype}

@defform[(deftype Name [fields ...] ProtocolName (method [params] body ...) ...)]{
Defines a type implementing one or more protocols.

@codeblock|{
(deftype Counter [n]
  IDeref
  (deref [this] n))
}|}

@section[#:tag "extend-type"]{extend-type}

@defform[(extend-type TypeName ProtocolName (method [params] body ...) ...)]{
Extends an existing type with protocol implementations.

@codeblock|{
(extend-type String
  Greetable
  (greet [this] (str "Hello, " this)))
}|}

@section[#:tag "defmulti"]{defmulti / defmethod}

@defform[(defmulti name dispatch-fn)]{
Defines a multimethod with the given dispatch function.}

@defform[(defmethod name dispatch-val [params] body ...)]{
Adds an implementation for a dispatch value.

@codeblock|{
(defmulti area :shape)
(defmethod area :circle [m]
  (* 3.14 (:radius m) (:radius m)))
}|}
