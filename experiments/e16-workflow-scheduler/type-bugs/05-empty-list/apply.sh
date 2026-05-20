#!/usr/bin/env bash
# Bug 05: skip the empty-tasks guard by making it always fall through
# Change: (if (empty? tasks) to (if false
sed -i 's/(if (empty? tasks)/(if false/' "$1/scheduler.bgl"
