# 017 — v0.5.0: packaging, doc infrastructure, release hygiene

**Date:** 2026-05-18

## Hypothesis

The language and toolchain are feature-complete for consumer use. The
remaining barrier is packaging and documentation: an external user
shouldn't need to read internal docs to get started, and mechanical
facts (test count, stdlib size) shouldn't drift across files.

## Changes

**Nix flake:**
- `stdenv.mkDerivation` builds beagle as a package (racket + babashka deps)
- `wrapProgram` patches PATH, `substituteInPlace` patches BEAGLE_DIR
- devShell via direnv (`.envrc` with `use flake`)
- `nix build` produces working CLI, `nix flake check` passes

**Documentation infrastructure:**
- `docs/prompts/consumers/` — full + distilled agent system prompts
- `docs/prompts/contributors/src.md` — canonical contributor reference
- `bin/beagle-docs-sync` — propagates test count, stdlib size, devlog
  count across CLAUDE.md, AGENTS.md, README.md, cheatsheet.md, todo.md
- README: new Prompts section documenting the prompt directory

**Stale fact cleanup (via docs-sync):**
- Stdlib: 607 → 666 across 5 files
- Test count: 370 → 399 across 4 files
- Experiment count left manual (editorial, not mechanical)

**Release hygiene:**
- v0.4.0 tagged at 58b7412 (was committed but never tagged)
- Devlog 014 already existed; just needed the tag

**Daemon portability:**
- `filesystem-change-evt` is Racket stdlib — uses kqueue on macOS,
  inotify on Linux, ReadDirectoryChangesW on Windows. No code changes.

## Result

`beagle init` generates correct consumer cheatsheet. `beagle-docs-sync`
is idempotent. Nix flake builds. All v0.5.0 checklist items complete
except the tag itself.

## Interpretation

The packaging story is now: `nix run`, `beagle init`, write code. The
doc infrastructure prevents the accretion problem — mechanical facts
have one source of truth each, and a script to propagate them.

## Next question

Is the consumer surface actually usable by someone who hasn't seen the
internals? The prompt files exist but haven't been tested in a fresh
agent session on a non-beagle project.
