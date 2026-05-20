#!/usr/bin/env bash
# Bug 07: swap adjacency and reverse-adj in DependencyGraph constructor
sed -i 's/(->DependencyGraph adjacency reverse-adj task-ids)/(->DependencyGraph reverse-adj adjacency task-ids)/' "$1/graph.bgl"
