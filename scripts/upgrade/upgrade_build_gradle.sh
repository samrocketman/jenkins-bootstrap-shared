#!/bin/bash
#Created by Sam Gleske
#Fri Apr 21 23:26:16 PDT 2017
#Ubuntu 16.04.2 LTS
#Linux 4.4.0-72-generic x86_64

function cleanup_temp_on() {
  [ -n "${TMPFILE}" ] && rm -f "${TMPFILE}"
  [ -n "${JENKINS_HEADERS_FILE}" ] && rm -f "${JENKINS_HEADERS_FILE}"
}
trap cleanup_temp_on EXIT

export JENKINS_WEB="http://localhost:8080"
export SCRIPT_LIBRARY_PATH="scripts"

if [ ! -f build.gradle ]; then
  echo "Not being run from repository root." 1>&2
  exit 1
fi

#GNU sed is required.  Homebrew on Mac installs GNU sed as gsed.
#Try to detect gsed; otherwise, fall back to just using sed.
[ -x "$(type -P gsed)" ] && SED=gsed || SED=sed
export SED

if [ -z "$(type -P jenkins-call-url)" ]; then
  {
    #do not subshell but group command output to stderr
    #See Compound Commands in bash man page.
    echo "Missing jenkins-call-url script."
    echo 'Download and add to $PATH:'
    echo 'https://github.com/samrocketman/home/blob/master/bin/jenkins-call-url'
  } 1>&2
  exit 1
fi

#set up Jenkins env vars
source scripts/upgrade/env.sh
export JENKINS_CALL_ARGS='-m POST http://localhost:8080/scriptText --data-string script= -d'

#upgrade jenkins.war and all plugins
jenkins-call-url scripts/upgrade/upgradeJenkinsAndPlugins.groovy

echo -n 'Waiting for Jenkins and plugins to be downloaded.'
while [ "$(jenkins-call-url scripts/upgrade/isUpgradeInProgress.groovy)" = 'true' ]; do
  sleep 3
  echo -n '.'
done
echo

if [ "$(jenkins-call-url scripts/upgrade/needsRestart.groovy)" = 'true' ]; then
  #restart Jenkins
  "${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" restart
  #wait for jenkins to become available
  "${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

  #set up Jenkins env vars post-restart
  source scripts/upgrade/env.sh
  export JENKINS_CALL_ARGS='-m POST http://localhost:8080/scriptText --data-string script= -d'
fi

#temp file will automatically be cleaned up on exit because of trap function
TMPFILE=$(mktemp)
echo "Using TMPFILE: $TMPFILE"

echo 'Upgrade build.gradle file.'
jenkins-call-url scripts/upgrade/generateSedExpr.groovy > "${TMPFILE}"
$SED -i.bak -rf "${TMPFILE}" build.gradle
rm build.gradle.bak

#search for plugins missing from build.gradle
jenkins-call-url scripts/upgrade/listShortName.groovy > "${TMPFILE}"

MISSING_PLUGINS=false
while read x; do
  grep -- "$x" build.gradle > /dev/null || {
    if ! ${MISSING_PLUGINS}; then
      echo 'Detected missing plugins:'
    fi
    echo -n "  Missing "
    #use bash Parameter Expansion to delete all : characters from $x
    jenkins-call-url scripts/upgrade/listSearchPom.groovy | grep "${x//:/}"
    MISSING_PLUGINS=true
  }
done < "${TMPFILE}"
if ${MISSING_PLUGINS}; then
  echo 'Missing plugins can be searched for at https://repo.jenkins-ci.org/'
fi
echo 'Done.'
