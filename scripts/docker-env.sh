
if ! type -P docker-compose || [ -z "$(docker-compose ps -q)" ]; then
  echo "ERROR: docker-compose does not exist or is not running" >&2
  exit 1
fi

if [ -z "${JENKINS_PASSWORD}" ]; then
  export JENKINS_PASSWORD=$(docker-compose ps -q | xargs -n1 -I {} docker exec -t {} cat /var/lib/jenkins/secrets/initialAdminPassword | sed 's/\r$//')
fi
