#!/usr/bin/env bash
# Differential oracle: same beagle source, zig vs babashka, byte-diffed.
set -euo pipefail
cd "$(dirname "$0")"
N="${1:-5000}"
SEED="${2:-12345}"
./zig-out/bin/kernel --dif "$N" --seed "$SEED" 2> /tmp/dif_zig.txt
(cd bb && bb --classpath . run_cases.clj "$N" "$SEED" > /tmp/dif_bb.txt)
if diff -q /tmp/dif_zig.txt /tmp/dif_bb.txt > /dev/null; then
    echo "differential: $N cases IDENTICAL (seed $SEED)"
else
    echo "differential: DIVERGENCE"
    diff /tmp/dif_zig.txt /tmp/dif_bb.txt | head -10
    exit 1
fi
