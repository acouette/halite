#!/bin/bash


rm -rf target
mkdir target
zip -r target/mybot.zip src/*
touch target/halite.custom.log
javac -d target -sourcepath src src/MyBot.java src/MyBot2.java src/RandomBot.java
cp halite target
cp oneGame.sh target
cd target

printf "\n30 30 Mybot vs RandomBot\n"
./oneGame.sh "40 40" "java MyBot" "java RandomBot"
#./oneGame.sh "30 30" "java MyBot" "java RandomBot"
#./oneGame.sh "30 30" "java MyBot" "java RandomBot"
#./oneGame.sh "30 30" "java MyBot" "java RandomBot"
#./oneGame.sh "30 30" "java MyBot" "java RandomBot"
#
#
#printf "\n30 30 Mybot vs MyBot2\n"
#./oneGame.sh "30 30" "java MyBot" "java MyBot2"
#./oneGame.sh "30 30" "java MyBot" "java MyBot2"
#./oneGame.sh "30 30" "java MyBot" "java MyBot2"
#./oneGame.sh "30 30" "java MyBot" "java MyBot2"
#./oneGame.sh "30 30" "java MyBot" "java MyBot2"
