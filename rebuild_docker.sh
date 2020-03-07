#! /bin/bash
set -e

./gradlew build
docker build -t my-application .
