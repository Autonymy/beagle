# kernel — tick kernel for the minds game (Phase 0)

The reference implementation the Zig backend is specified against
(life-os thread `20260612232001`). ~300 agent minds on a chunked voxel
heightfield: belief EMA + social alarm contagion + alarm-escalation
state machine; panicked minds burrow (the voxel-edit commit path).

## Memory model (brief §2 + voxel amendment)

- **tick arena** — all per-tick temporaries; reset once per tick by the
  harness (`.free_all` in Debug so cross-tick retention = detected
  use-after-free; `.retain_capacity` in release). Kernel code never
  frees.
- **mind state** — double-buffered SoA; tick reads `read` immutably,
  commit writes the next state, swap.
- **voxel grid** — third store: preallocated chunks, read-only during
  the tick; dig edits accumulate in the arena and are applied in
  deterministic order at commit (chunks marked dirty).

## Toolchain

Zig **master-nightly via zig-overlay** (flake-pinned; at adoption
`0.17.0-dev.813+2153f8143` — bump with `nix flake update zig-overlay`).
Renderer: **sokol** (official sokol-zig bindings, commit-pinned in
`build.zig.zon`); one instanced-cube pipeline draws terrain + minds in
a single draw call (hand-written GLSL 410, GL core backend; sokol-shdc
when a second platform appears). `use_llvm/use_lld` are forced: zig
master's self-hosted ELF linker currently trips on sokol's libGL
reference.

## Run

    zig build run                      # window, auto-orbit camera
    zig build run -- --headless 10000  # conformance run, prints hash
    ./zig-out/bin/kernel --headless 1000 --seed 0x42

Same seed → same hash, every run. The hash folds every mind's decision,
alarm, and position plus every applied voxel edit, per tick — it is the
fingerprint the Phase 1 emitted code and the Babashka differential
oracle must reproduce exactly.

Phase 0 acceptance (2026-06-12): 10k headless ticks in 5.8s under the
Debug allocator, zero leaks, deterministic
`hash=0x5A73651575B8F2C3` (seed 0xBEA61E).

## Phase 1 — the kernel is beagle-authored (2026-06-13)

`src/sim_kernel.bgl` is the source of truth for every per-mind rule.
`./build-sim.sh` compiles it through TWO backends:

    zig -> src/sim.zig          (replaces the handwritten Phase 0 file)
    clj -> bb/sim_kernel.clj    (the babashka differential oracle)

Acceptance, both green:
- `--headless 10000` hash is IDENTICAL to the Phase 0 handwritten
  kernel: 0x5A73651575B8F2C3 — every decision, alarm, position, voxel
  edit, and rng draw preserved across the rewrite.
- `./differential.sh 5000` — same beagle source run on zig and
  babashka with a shared Splitmix64 stream: byte-identical outputs.

Golden snapshots (26 modules, each byte-pinned AND zig-compiled) live
in beagle-test/tests/fixtures/zig-golden/ (suite: emit-zig.rkt;
re-bless with BEAGLE_ZIG_BLESS=1 after reviewed emitter changes).

