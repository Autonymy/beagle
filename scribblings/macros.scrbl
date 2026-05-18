#lang scribble/manual

@title[#:tag "macros"]{Macros}

@section[#:tag "define-macro"]{define-macro}

@defform[(define-macro safe name (params) template)]{
Defines a macro whose expansion is type-checked normally.}

@defform[#:id define-macro-unsafe (define-macro unsafe name (params) template)]{
Defines a macro whose expansion is typed as @tt{Any} (escape boundary).

@codeblock|{
(define-macro safe inc1 (x)
  (+ x 1))

(define-macro safe call-with (f & args)
  (f (splice args)))

(define-macro unsafe debug-call (form)
  (do (println "trace") form))
}|

@itemlist[
  @item{@tt{safe}: expansion re-validated by type checker}
  @item{@tt{unsafe}: expansion's result type widened to @tt{Any}}
  @item{@tt{& rest-name} in params: collects remaining args into a list}
  @item{@tt{(splice rest-name)} in template: inlines the list at that position}
  @item{@tt{safe} macros use gensym-hygienic substitution; @tt{unsafe} macros
        use naive substitution}
]}
