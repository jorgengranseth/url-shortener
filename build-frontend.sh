#! /bin/sh
# docker-compose up -d elm
cd frontend

npx elm make --optimize src/Main.elm --output="../resources/static/index.html"
