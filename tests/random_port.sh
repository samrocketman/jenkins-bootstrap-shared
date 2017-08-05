#!/bin/bash
#Created by Sam Gleske
#Chooses a random open port from 10000-20000

while true; do
  PORT="$(( ($RANDOM % 10000) + 10000 ))"
  if ! timeout 5 nc -z localhost $PORT &> /dev/null; then
    echo $PORT
    break
  fi
done
