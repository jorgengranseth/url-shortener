# Ktor on Heroku with Docker

Experiment to run Ktor in a Docker container on Heroku.

## Setup

```
heroku create
heroku addons:create heroku-postgresql
heroku config:set IS_HEROKU=true
```
