//! Test-support runtime module for the zig-golden compile check.
//!
//! Golden snapshots that declare los.rt/* externs now lower to a SEPARATE
//! `los_rt` Zig module (Phase 1 runtime-module separation), so their
//! compile check needs a `los_rt.zig` beside them. This is a minimal,
//! self-contained stand-in providing exactly the los.rt externs the
//! golden fixtures exercise — the real application runtime lives in
//! ~/code/life-os/los-bb/zig/los_rt.zig and is verified by los parity,
//! not by this fixture. Keep this in sync with the los.rt externs any
//! golden under zig-golden/ declares.

const std = @import("std");

var arena_state: ?std.heap.ArenaAllocator = null;
fn alloc() std.mem.Allocator {
    if (arena_state == null) arena_state = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    return arena_state.?.allocator();
}

pub fn split_on(s: []const u8, sep: []const u8) []const []const u8 {
    if (sep.len == 0) return &.{s};
    var n: usize = 0;
    var c = std.mem.splitSequence(u8, s, sep);
    while (c.next()) |_| n += 1;
    const out = alloc().alloc([]const u8, n) catch @panic("oom");
    var it = std.mem.splitSequence(u8, s, sep);
    var j: usize = 0;
    while (it.next()) |part| : (j += 1) out[j] = part;
    return out;
}
pub fn repeat_str(s: []const u8, n: i64) []const u8 {
    const k: usize = if (n < 0) 0 else @intCast(n);
    const out = alloc().alloc(u8, s.len * k) catch @panic("oom");
    var i: usize = 0;
    while (i < k) : (i += 1) @memcpy(out[i * s.len ..][0..s.len], s);
    return out;
}
pub fn error_exit(msg: []const u8) noreturn {
    std.debug.print("error: {s}\n", .{msg});
    std.process.exit(1);
}
pub fn epoch_seconds() i64 {
    return std.time.timestamp();
}
