#lang scribble/manual

@title[#:tag "getting-started"]{Getting Started}

@section{Installation}

Install beagle as a linked Racket package:

@verbatim|{
  raco pkg install --link --auto /path/to/beagle
}|

If using Nix, the flake provides a dev shell:

@verbatim|{
  echo 'use flake' > .envrc && direnv allow
}|

@section{File Structure}

A beagle source file uses the @tt{#lang beagle} declaration:

@codeblock|{
#lang beagle

(ns example.demo)              ; namespace (default: beagle.user)
(define-mode strict)           ; default; or dynamic to skip type checks
(require some.module :as mod)  ; import types/fns from another beagle module
(declare-extern fn [A -> R])   ; only for Java interop or non-beagle fns
(import java.io.File)          ; Java class import

;; definitions follow...
(def greeting : String "hello")

(defn add [(x : Long) (y : Long)] : Long
  (+ x y))
}|

Meta forms (@tt{ns}, @tt{define-mode}, @tt{require}, @tt{declare-extern},
@tt{define-macro}, @tt{import}) can appear anywhere but conventionally
go at the top.

@section{Compiling and Checking}

@itemlist[
  @item{@tt{beagle check .} --- type-check all files in the current directory}
  @item{@tt{beagle build . --out .build/} --- compile beagle to Clojure source}
  @item{@tt{beagle fix --apply .} --- auto-fix mechanical type errors}
  @item{@tt{beagle sig fn-name .} --- query a function's type signature}
  @item{@tt{beagle repl} --- interactive REPL with type checking}
  @item{@tt{beagle lsp} --- LSP server for editor integration}
]

@section{Cross-Module Imports}

@tt{(require module :as alias)} imports all typed definitions, records,
scalars, and macros from another beagle module. No @tt{declare-extern}
is needed for cross-module beagle calls:

@codeblock|{
(require inventory :as inv)

;; Type checker knows: inv/can-fulfill? : [(Vec StockLevel) Long Long -> Boolean]
(inv/can-fulfill? levels product-id qty)
}|

For non-beagle namespaces (Clojure libraries), use @tt{declare-extern}
for type-checked calls, or accept @tt{Any}-typed pass-through.
