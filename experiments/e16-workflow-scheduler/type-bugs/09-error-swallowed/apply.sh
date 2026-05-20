#!/usr/bin/env bash
# Bug 09: always return Ok, even when there are failures
sed -i 's/(if (empty? final-failures)/(if true/' "$1/scheduler.bgl"
