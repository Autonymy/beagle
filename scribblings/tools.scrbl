#lang scribble/manual

@title[#:tag "tools"]{Tools}

@section{Unified CLI}

The @tt{beagle} command wraps all common operations:

@itemlist[
  @item{@tt{beagle check .} --- batch type-check all files}
  @item{@tt{beagle build . --out DIR} --- batch compile to Clojure}
  @item{@tt{beagle fix --dry-run|--apply .} --- auto-fix type errors}
  @item{@tt{beagle sig FN .} --- query function signature}
  @item{@tt{beagle lsp} --- LSP server (stdio transport)}
  @item{@tt{beagle repl} --- typed REPL with persistent environment}
  @item{@tt{beagle init} --- bootstrap a new beagle project}
]

@section{Reactive Daemon}

The daemon caches ASTs and watches files for changes, providing near-instant
re-checking on every save:

@itemlist[
  @item{@tt{beagle-daemon start --watch DIR} --- file watcher, re-checks on
        every save (~100ms)}
  @item{@tt{beagle-daemon query check-enriched DIR} --- synchronous type
        check + enriched context}
  @item{@tt{beagle-daemon query check-result FILE} --- cached result (instant)}
]

@section{Query Tools}

Query tools expose the type system as an API. Instead of reading source
to understand a codebase, ask the type system directly:

@itemlist[
  @item{@tt{beagle-sig FN FILE-OR-DIR} --- function's type signature}
  @item{@tt{beagle-fields RECORD FILE-OR-DIR} --- record fields + accessors}
  @item{@tt{beagle-callers FN FILE-OR-DIR} --- find all call sites}
  @item{@tt{beagle-provides FILE-OR-DIR} --- list module exports with types}
  @item{@tt{beagle-impact FN FILE-OR-DIR} --- callers + impact of signature change}
]

@section{MCP Server}

Exposes beagle's type system as tools over the Model Context Protocol,
so any MCP-compatible agent gets type-aware code intelligence:

@verbatim|{
  beagle mcp
}|

Tools: @tt{beagle_sig}, @tt{beagle_fields}, @tt{beagle_callers},
@tt{beagle_provides}, @tt{beagle_impact}, @tt{beagle_check},
@tt{beagle_check_enriched}, @tt{beagle_build}, @tt{beagle_expand}.
Delegates to a running daemon for speed; falls back to direct CLI invocation.

Requires the @tt{mcp} Python package (@tt{pip install mcp}).

@section{Repair Toolchain}

Automated bug-finding and fixing tools that use oracle-based behavioral
comparison, tracing, and cross-evidence correlation:

@itemlist[
  @item{@tt{beagle-repair SOURCE VERIFY [--auto] [--emit-patch]} --- unified
        repair pipeline with cross-evidence correlation}
  @item{@tt{beagle-trace BUILD VERIFY [--focus FN]} --- instrumented tracing}
  @item{@tt{beagle-specfix BUILD VERIFY} --- oracle-guided speculative fix}
  @item{@tt{beagle-cascade SOURCE VERIFY --from-failures} --- root-cause analysis}
  @item{@tt{beagle-blame BUILD VERIFY} --- ratio-based fault hints}
  @item{@tt{beagle-oracle GOLDEN [--diff MODIFIED]} --- behavioral oracle synthesis}
  @item{@tt{beagle-proptest SOURCE [--run] [--diff DIR]} --- property + differential testing}
]

@section{Other Tools}

Individual tools for specific tasks, including batch compilation, syntax
checking, mutation testing, and distributed tracing:

@itemlist[
  @item{@tt{beagle-build SOURCE.rkt [OUT.clj]} --- single-file compile}
  @item{@tt{beagle-build-all FILES [--out DIR]} --- batch compile (9x vs sequential)}
  @item{@tt{beagle-check SOURCE.rkt} --- single-file type check}
  @item{@tt{beagle-check-all FILES} --- batch type check (10x vs sequential)}
  @item{@tt{beagle-expand SOURCE.rkt} --- show post-macro expansion}
  @item{@tt{beagle-syntax FILES} --- fast paren/bracket balance check (<200ms)}
  @item{@tt{beagle-verify-enriched BUILD VERIFY} --- verify + auto-diagnose}
  @item{@tt{beagle-muttest BUILD VERIFY} --- mutation testing}
  @item{@tt{beagle-dtrace instrument|collect|view|blame|graph|cascade} --- distributed tracing}
  @item{@tt{beagle-smap extract|compose} --- source map generation}
  @item{@tt{beagle-docs-sync} --- propagate mechanical facts across docs}
]
