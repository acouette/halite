#!/bin/bash

rm *.hlt
rm *.class
rm *.log

zip mybot.zip *.java


javac MyBot.java
javac RandomBot.java
./halite -d "30 30" "java MyBot" "java RandomBot"
