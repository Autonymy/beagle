#!/usr/bin/env bash
# Bug 04: swap task-id and worker-id in assignment constructor
# Target: the ->Assignment call inside ->AttemptSuccess in schedule-with-retry
# Step 1: On the ->Assignment line, swap (task-id task) to (worker-id worker)
# Step 2: On the next line (originally worker-id), swap to (task-id task)
sed -i '/->Assignment/s/(task-id task)/(worker-id worker)/' "$1/scheduler.bgl"
sed -i '/->Assignment/{n;s/(worker-id worker)/(task-id task)/;}' "$1/scheduler.bgl"
