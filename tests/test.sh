#!/bin/bash -e
export LC_ALL=C
export PS4='$ '
export DEBIAN_FRONTEND=noninteractive
if [ ! -d ".git" ]; then
  echo 'ERROR: must be run from the root of the repository.  e.g.'
  echo './tests/test.sh'
  exit 1
fi
if [ -z "${GITHUB_TOKEN}" ]; then
  echo 'WARNING: Tests may fail without GITHUB_TOKEN environment variable set.'
fi
echo 'Last command to exit has the non-zero exit status.'

#configure error exit trap
function on_err() {
  set +x
  echo "^^ command above has non-zero exit code."
  echo
  echo "Cleaning up test environment: ./gradlew clean"
  ./gradlew clean &> /dev/null
}
trap on_err ERR

set -x

bash -n ./tests/random_port.sh
test -x ./tests/random_port.sh
export RANDOM_PORT="$(./tests/random_port.sh)"
bash -n ./jervis_bootstrap.sh
test -x ./jervis_bootstrap.sh
export JENKINS_START="java -jar jenkins.war --httpPort=${RANDOM_PORT} --httpListenAddress=127.0.0.1"
export JENKINS_WEB="http://127.0.0.1:${RANDOM_PORT}"
export JENKINS_CLI="java -jar ./jenkins-cli.jar -s http://127.0.0.1:${RANDOM_PORT} -noKeyAuth"
export JENKINS_HOME="$(mktemp -d ../my_jenkins_homeXXX)"
./jervis_bootstrap.sh
set +x

#Jenkins 2.0
source scripts/common.sh
CURL='curl'
#try enabling authentication
if is_auth_enabled; then
  export CURL="${CURL} -u admin:$(<${JENKINS_HOME}/secrets/initialAdminPassword)"
fi
#try enabling CSRF protection support
csrf_set_curl
set -x

$CURL -s "${JENKINS_WEB}/api/json?pretty=true" | python ./tests/api_test.py
$CURL -X POST "${JENKINS_WEB}/job/_jervis_generator/build?delay=0sec" --data-urlencode json='{"parameter": [{"name":"project", "value":"samrocketman/jervis"}, {"name":"branch", "value":""}]}'
sleep 1
python ./tests/job_test.py "${JENKINS_WEB}/job/_jervis_generator/lastBuild/api/json?pretty=true"
test -e "$JENKINS_HOME"/jobs/samrocketman/jobs/jervis-master/config.xml
test -e jenkins.pid
./scripts/provision_jenkins.sh stop
test ! -e jenkins.pid
test -e console.log
test -e jenkins.war
test -e "$JENKINS_HOME"
test -e plugins
./gradlew clean
test ! -e console.log
test ! -e jenkins.war
test ! -e "$JENKINS_HOME"
test ! -e plugins
