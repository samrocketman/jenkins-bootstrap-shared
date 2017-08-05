#!/bin/bash
#Created by Sam Gleske
#Fri Apr 21 23:26:16 PDT 2017
#Ubuntu 16.04.2 LTS
#Linux 4.4.0-72-generic x86_64

set -e

function cleanup_temp_on() {
  [ -n "${TMPFILE}" ] && rm -f "${TMPFILE}"
  [ -n "${GAV_TMPFILE}" ] && rm -f "${GAV_TMPFILE}"
  [ -n "${JENKINS_HEADERS_FILE}" ] && rm -f "${JENKINS_HEADERS_FILE}"
}
trap cleanup_temp_on EXIT

source env.sh

#protect user from accidentally upgrading a remote Jenkins
#this should always be localhost
if [ ! "${JENKINS_WEB}" = 'http://localhost:8080' ]; then
  echo 'ERROR: JENKINS_WEB is not equal to localhost' >&2
  echo "JENKINS_WEB = ${JENKINS_WEB}" >&2
  exit 1
fi

if [ ! -f build.gradle ]; then
  echo "Not being run from repository root." 1>&2
  exit 1
fi

#set up Jenkins env vars
source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
export JENKINS_CALL_ARGS="-m POST ${JENKINS_WEB}/scriptText --data-string script= -d"

#upgrade jenkins.war and all plugins
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/upgradeJenkinsAndPlugins.groovy

echo -n 'Waiting for Jenkins and plugins to be downloaded.'
while [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/isUpgradeInProgress.groovy)" = 'true' ]; do
  sleep 3
  echo -n '.'
done
echo

if [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/needsRestart.groovy)" = 'true' ]; then
  #restart Jenkins
  "${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh restart
  #wait for jenkins to become available
  "${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

  #set up Jenkins env vars post-restart
  source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
  export JENKINS_CALL_ARGS="-m POST ${JENKINS_WEB}/scriptText --data-string script= -d"
fi

#write jenkins master and plugin versions to build.gradle and dependencies.gradle
"${SCRIPT_LIBRARY_PATH}"/upgrade/remote_dependencies.sh
