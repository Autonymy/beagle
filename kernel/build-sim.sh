#!/usr/bin/env bash
# Regenerate src/sim.zig + bb/sim_kernel.clj from src/sim_kernel.bgl.
set -euo pipefail
cd "$(dirname "$0")"
exec racket build-sim.rkt
