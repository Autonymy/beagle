#!/usr/bin/env bash
# Bug 02: >= instead of > in retry loop termination
sed -i 's/(if (> attempt max-attempts)/(if (>= attempt max-attempts)/' "$1/scheduler.bgl"
