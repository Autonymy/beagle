#!/usr/bin/env bash
# Bug 08: swap tie-breaking order from [priority, id] to [id, priority]
sed -i 's/\[(task-priority task) tid\]/[tid (task-priority task)]/g' "$1/graph.bgl"
