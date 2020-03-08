#! /bin/bash
set -e

./gradlew build --stacktrace
heroku container:push web
heroku container:release web
