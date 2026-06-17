#!/usr/bin/env bash
# move 4 — the authoring substrate: a structured edit (the kind an agent/LLM emits
# as DATA) becomes a RECOMPILE-GATED TRANSACTION on the canonical claim store.
#
# Apply the edit as a claim mutation -> regenerate byte-stable -> recompile; COMMIT
# (write the new tree) only if it builds clean, else REJECT (fail closed). This is
# "dictate intent (structured) -> validated claim edit -> code falls out, safely."
# The deterministic substrate an agent drives — proven via the rename + cascade ops
# + the collision invariant + this recompile gate. The NL -> structured-edit layer
# (turning prose into one of these edit specs) is the model's job, not gated here.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
RT="$ROOT/beagle-lib/private/claims-roundtrip.rkt"
FRAM_OUT="${FRAM_OUT:-$HOME/code/fram/out}"
fail=0

# author <outdir> <corpus> <op> <op-args...>  — prints COMMITTED or REJECTED.
# A claim edit that the op refuses (collision) OR that does not recompile is
# REJECTED with no tree written (fail-closed); a valid one commits the new tree.
author() {
  local outdir="$1" corpus="$2" op="$3"; shift 3
  local W; W="$(mktemp -d)"; local E="$W/edn"; mkdir -p "$E" "$W/regen"
  local edns=() f b
  for f in "$corpus"/*.bclj; do b="$(basename "$f")"; racket "$RT" --emit-edn "$f" 2>/dev/null > "$E/$b.edn"; edns+=("$E/$b.edn"); done
  case "$op" in
    rename)  bb -cp "$FRAM_OUT" "$HERE/rename-in-store.clj" "$1" "$2" "$3"      "$W/out" "${edns[@]}" >/dev/null 2>&1 || { echo REJECTED; rm -rf "$W"; return; } ;;
    cascade) bb -cp "$FRAM_OUT" "$HERE/rename-cascade.clj"  "$1" "$2" "$3" "$4" "$W/out" "${edns[@]}" >/dev/null 2>&1 || { echo REJECTED; rm -rf "$W"; return; } ;;
    *) echo "REJECTED"; rm -rf "$W"; return ;;
  esac
  for f in "$corpus"/*.bclj; do b="$(basename "$f")"; racket "$RT" --render "$W/out/$b.edn" 2>/dev/null > "$W/regen/$b"; done
  if "$ROOT/bin/beagle-build-all" "$W/regen" --out "$W/o" 2>&1 | grep -q '0 error'; then
    rm -rf "$outdir"; cp -r "$W/regen" "$outdir"; echo COMMITTED
  else
    echo REJECTED
  fi
  rm -rf "$W"
}

echo "================ move 4 — authoring substrate (recompile-gated edit transaction) ================"
[ -d "$FRAM_OUT" ] || { echo "  (need FRAM_OUT)"; exit 3; }
C="$HERE/rename-corpus"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT

echo "--- a valid structured edit COMMITS and recompiles ---"
r="$(author "$T/v" "$C" rename helper renamed-fn mod_a)"
if [ "$r" = COMMITTED ] && grep -q 'renamed-fn' "$T/v/mod_a.bclj" 2>/dev/null && ! grep -q 'helper' "$T/v/mod_a.bclj"; then
  echo "  PASS  valid edit committed (tree written, edit applied, recompiled clean)"
else echo "  FAIL  expected COMMITTED, got '$r'"; fail=1; fi

echo "--- an invalid edit (collision) is REJECTED, no tree written (fail closed) ---"
r="$(author "$T/i" "$C" rename helper use-a mod_a)"   # use-a already binds in mod_a
if [ "$r" = REJECTED ] && [ ! -d "$T/i" ]; then
  echo "  PASS  colliding edit rejected, nothing committed"
else echo "  FAIL  expected REJECTED+no-commit, got '$r' (dir exists: $([ -d "$T/i" ] && echo yes || echo no))"; fail=1; fi

echo "--- the other module is untouched in the committed tree (scope-correct) ---"
if grep -q 'helper' "$T/v/mod_b.bclj" 2>/dev/null && ! grep -q 'renamed-fn' "$T/v/mod_b.bclj"; then
  echo "  PASS  mod_b's helper preserved in the committed result"
else echo "  FAIL  mod_b not scope-correct in commit"; fail=1; fi

echo
if [ "$fail" = 0 ]; then
  echo "RESULT: PASS — structured edits are recompile-gated transactions: valid commits, invalid fails closed."
else
  echo "RESULT: FAIL"; exit 1
fi
