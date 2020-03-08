#! /bin/bash
set -e

./rebuild_docker.sh
heroku container:push web
heroku container:release web
