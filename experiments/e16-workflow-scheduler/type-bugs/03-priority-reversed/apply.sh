#!/usr/bin/env bash
# Bug 03: negate priority in topo sort so higher numbers go first
# Targets both sort-by blocks in topological-order
sed -i 's/\[(task-priority task) tid\]/[(- 0 (task-priority task)) tid]/g' "$1/graph.bgl"
