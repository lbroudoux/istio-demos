#!/bin/bash 
set -eu 

URL="http://$(oc get route | grep greeter-service | awk '{print $2}')"

while true
do
  curl $URL/api/greet/Laurent && echo ''
  sleep 2
done;