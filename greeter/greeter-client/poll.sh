#!/bin/bash 
set -eu 

URL="http://$(oc get route | grep greeter-client | awk '{print $2}')"

while true
do
  curl $URL/api/greet/Laurent
  sleep 1
done;