#!/bin/bash

if ! type -P docker-compose || [ -z "$(docker-compose ps -q)" ]; then
  echo "ERROR: docker-compose does not exist or is not running" >&2
  exit 1
fi

function password_ready() {
  docker-compose ps -q | \
  xargs -n1 -I {} docker exec {} test -r "${DOCKER_JENKINS_HOME:-/var/lib/jenkins}"/secrets/initialAdminPassword
}


if [ -z "${JENKINS_PASSWORD}" ]; then
  until password_ready; do
    echo "${DOCKER_JENKINS_HOME:-/var/lib/jenkins}/secrets/initialAdminPassword not available, yet..."
    sleep 5
  done
  JENKINS_PASSWORD="$(docker-compose ps -q | xargs -n1 -I {} docker exec {} cat "${DOCKER_JENKINS_HOME:-/var/lib/jenkins}"/secrets/initialAdminPassword)"
  export JENKINS_PASSWORD
fi
