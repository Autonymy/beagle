//! Deterministic primitives. No wall clock, no host RNG, anywhere.
//!
//! Splitmix64 lives in the prelude (beagle_rt.zig) — ONE canonical
//! implementation shared nominally by harness and emitted code through
//! rt.Ctx; the Babashka prelude mirrors it with unchecked 64-bit ops so
//! the differential oracle draws identical streams.

pub const Splitmix64 = @import("beagle_rt.zig").Splitmix64;

/// FNV-1a, 64-bit. The conformance fingerprint: every decision and every
/// voxel edit of every tick folds through this. Identical streams of
/// folds => identical hash, on every backend.
pub const Fnv1a = struct {
    h: u64 = 0xCBF29CE484222325,

    pub fn foldU64(self: *Fnv1a, v: u64) void {
        var x = v;
        var i: usize = 0;
        while (i < 8) : (i += 1) {
            self.h = (self.h ^ (x & 0xFF)) *% 0x100000001B3;
            x >>= 8;
        }
    }

    pub fn foldI64(self: *Fnv1a, v: i64) void {
        self.foldU64(@bitCast(v));
    }
};
