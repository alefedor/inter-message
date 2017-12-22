#!/bin/bash

set -e

rm XX/work/src -rf
mkdir XX/work/src
for elem in $(cat XX/files.list)
do
    mkdir -p $(dirname XX/work/src/$elem)
    cp app/src/main/java/$elem XX/work/src/$elem
done

cp XX/Main.java XX/work/src/Main.java
cd XX/work
gradle build
