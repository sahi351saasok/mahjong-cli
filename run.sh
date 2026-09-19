#!/bin/sh

if [ $# -ne 1 ]; then
	echo "USAGE: <times>"
	exit 1
fi

n=$1

echo "play $n times"

for i in `seq $n`
do
  echo "No. $i"
  ./gradlew --configuration-cache run
done

