#!/usr/bin/env bash
# Bug 10: read start-time instead of end-time for dependency completion
# Target: the reduce in compute-earliest-start ONLY (not schedule-makespan)
# Uses function-scoped range to avoid over-matching the second assignment-end-time
sed -i '/^(defn compute-earliest-start/,/^(defn /{s/(max acc (assignment-end-time a))/(max acc (assignment-start-time a))/}' "$1/scheduler.bgl"
