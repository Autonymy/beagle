#!/usr/bin/env bash
# Bug 01: change < to <= in window overlap check
# matcher.bgl line 47: (and (< start1 end2) (< start2 end1))
sed -i 's/(and (< start1 end2) (< start2 end1))/(and (<= start1 end2) (< start2 end1))/' "$1/matcher.bgl"
