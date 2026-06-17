#!/usr/bin/env bash
# Cross-file rename-CASCADE — "rename a def, fix all readers across files."
#
# Renames a function defined in one module AND every cross-module reference to it,
# as a single claim edit on the canonical store: the unqualified name in its home
# module (def + intra-module callers) + the `<alias>/name` qualified refs in every
# module that aliased it via (require <ns> :as <alias>). Same-prefix-but-different
# symbols (value-id, value-object?) and any other module's own same-named symbol
# are untouched — scope is resolved through the require graph, not text.
#
# Proven on the real fram engine: rename fram.cnf/value! -> intern! across fram/src,
# then recompile clean. Needs racket + bb + fram out/ + the fram source corpus.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
RT="$ROOT/beagle-lib/private/claims-roundtrip.rkt"
RC="$HERE/rename-cascade.clj"
FRAM_OUT="${FRAM_OUT:-$HOME/code/fram/out}"
SRC="${CODE_AS_CLAIMS_CORPUS:-$HOME/code/fram/src}"
fail=0

echo "================ cross-file rename-cascade — rename a def, fix all readers ================"
echo "corpus: $SRC   edit: rename fram.cnf/value! -> intern! across all modules"
[ -d "$FRAM_OUT" ] || { echo "  (need FRAM_OUT)"; exit 3; }

W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
E="$W/edn"; mkdir -p "$E" "$W/regen/fram"
edns=()
while IFS= read -r f; do
  b="$(basename "$f")"; racket "$RT" --emit-edn "$f" 2>/dev/null > "$E/$b.edn"; edns+=("$E/$b.edn")
done < <(find "$SRC" -name '*.bclj' | sort)

bb -cp "$FRAM_OUT" "$RC" value! intern! cnf fram.cnf "$W/out" "${edns[@]}" 2>&1 | sed 's/^/  /'
while IFS= read -r f; do
  b="$(basename "$f")"; racket "$RT" --render "$W/out/$b.edn" 2>/dev/null > "$W/regen/fram/$b"
done < <(find "$SRC" -name '*.bclj' | sort)

chk() { if eval "$2"; then echo "  PASS  $1"; else echo "  FAIL  $1"; fail=1; fi; }
echo "--- the cross-file edit ---"
chk "home module: def renamed (defn intern!)"          "grep -q 'defn intern!' '$W/regen/fram/cnf.bclj'"
chk "home module: no 'defn value!' left"               "! grep -q 'defn value!' '$W/regen/fram/cnf.bclj'"
chk "NO '/value!' qualified ref left anywhere"         "! grep -rqh '/value!' '$W/regen/fram/'"
chk "'/intern!' qualified refs now present"            "grep -rqh '/intern!' '$W/regen/fram/'"
echo "--- scope-correctness (same-prefix different symbols untouched) ---"
chk "value-id still present (NOT renamed)"             "grep -rqh 'value-id' '$W/regen/fram/'"
chk "value-object? still present (NOT renamed)"        "grep -rqh 'value-object' '$W/regen/fram/'"
echo "--- the cascaded program recompiles ---"
if "$ROOT/bin/beagle-build-all" "$W/regen" --out "$W/o" 2>&1 | grep -q '0 error'; then
  echo "  PASS  cascaded tree builds clean ($(find "$W/o" -name '*.clj' | wc -l) modules)"
else
  echo "  FAIL  cascaded tree does not build"; "$ROOT/bin/beagle-build-all" "$W/regen" --out "$W/o2" 2>&1 | tail -3; fail=1
fi

echo
if [ "$fail" = 0 ]; then
  echo "RESULT: PASS — cross-file cascade: def + all qualified readers renamed, scope-correct, recompiles."
else
  echo "RESULT: FAIL"; exit 1
fi
