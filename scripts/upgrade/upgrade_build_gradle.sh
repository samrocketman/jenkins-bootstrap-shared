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

if [ ! -f build.gradle ]; then
  echo "Not being run from repository root." 1>&2
  exit 1
fi

#GNU sed is required.  Homebrew on Mac installs GNU sed as gsed.
#Try to detect gsed; otherwise, fall back to just using sed.
[ -x "$(type -P gsed)" ] && SED=gsed || SED=sed
export SED

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

#temp file will automatically be cleaned up on exit because of trap function
TMPFILE=$(mktemp)
GAV_TMPFILE=$(mktemp)

echo 'Upgrade build.gradle file.'
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/generateSedExpr.groovy > "${TMPFILE}"
$SED -i.bak -rf "${TMPFILE}" build.gradle
rm build.gradle.bak

#search for plugins missing from build.gradle
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/listShortNameVersion.groovy > "${TMPFILE}"

MISSING_PLUGINS=false
while read x; do
  grep -- ":${x%:*}:" build.gradle > /dev/null || {
    if ! ${MISSING_PLUGINS}; then
      echo 'Detected missing plugins (add to your build.gradle):'
      "${SCRIPT_LIBRARY_PATH}"/upgrade/plugins_gav.sh > "${GAV_TMPFILE}"
    fi
    local GROUP=$(awk "BEGIN {FS=':'};\$2 == '${x%:*}' { print \$1 }" "${GAV_TMPFILE}")
    echo "    getplugins '${GROUP}:${x}@hpi'"
    MISSING_PLUGINS=true
  }
done < "${TMPFILE}"
if ${MISSING_PLUGINS}; then
  echo 'Missing plugins can be searched for at https://repo.jenkins-ci.org/'
fi
echo 'Done.'
