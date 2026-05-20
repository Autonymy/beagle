#!/usr/bin/env bash
# Bug 06: remove the AttemptFailure match arm from process-single-task
# The match currently handles both AttemptSuccess and AttemptFailure.
# Removing AttemptFailure makes the match non-exhaustive (caught by P2+ checker).
# 1. Append closing parens to the AttemptSuccess arm (to close match + outer exprs)
# 2. Delete the AttemptFailure pattern line (no closing ] on this line)
# 3. Delete the AttemptFailure body line (which had the original closing parens)
sed -i 's/(assoc acc :assignments (conj assignments-so-far a))\]/(assoc acc :assignments (conj assignments-so-far a))])))))))/' "$1/scheduler.bgl"
sed -i '/\[(AttemptFailure f)/d' "$1/scheduler.bgl"
sed -i '/assoc acc :failures (conj failures-so-far f)/d' "$1/scheduler.bgl"
