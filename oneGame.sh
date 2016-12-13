#!/bin/bash

./halite -d "$@" | grep "Couettos, came" | sed -e 's/Player #1, Couettos, came in rank #\(.*\) and was last alive on frame #\(.*\)!/\1/'