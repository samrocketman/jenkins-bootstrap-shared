#!/bin/bash
#Created by Sam Gleske
#Fri Apr 21 23:26:16 PDT 2017
#Ubuntu 16.04.2 LTS
#Linux 4.4.0-72-generic x86_64

function cleanup_on() {
  #clean up jenkins headers file
  [ -n "${JENKINS_HEADERS_FILE}" -a -f "${JENKINS_HEADERS_FILE}" ] && rm -f "${JENKINS_HEADERS_FILE}"
  [ -n "${VAGRANT_SSH_CONFIG}" -a -f "${VAGRANT_SSH_CONFIG}" ] && rm -f "${VAGRANT_SSH_CONFIG}"
  if [ "$1" = '0' ]; then
    echo "Jenkins is ready.  Visit ${JENKINS_WEB}/"
    echo "User: ${JENKINS_USER}"
    echo "Password: ${JENKINS_PASSWORD}"
  fi
}
trap 'cleanup_on $?' EXIT

set -e

function cleanup_temp_on() {
  [ -n "${TMPFILE}" ] && rm -f "${TMPFILE}"
  [ -n "${GAV_TMPFILE}" ] && rm -f "${GAV_TMPFILE}"
  [ -n "${JENKINS_HEADERS_FILE}" ] && rm -f "${JENKINS_HEADERS_FILE}"
}
trap cleanup_temp_on EXIT

source env.sh
#set password if using vagrant
[ -n "${VAGRANT_JENKINS}" ] && source "${SCRIPT_LIBRARY_PATH}/vagrant-env.sh"
[ -n "${DOCKER_JENKINS}" ] && source "${SCRIPT_LIBRARY_PATH}/docker-env.sh"

#allow users to import when using no upgrade
[ -n "${NO_UPGRADE}" ] && FORCE_UPGRADE=1

#protect user from accidentally upgrading a remote Jenkins
#this should always be localhost
if [ -z "${FORCE_UPGRADE}" -a ! "${JENKINS_WEB}" = 'http://localhost:8080' ]; then
  echo 'ERROR: JENKINS_WEB is not equal to localhost' >&2
  echo "JENKINS_WEB = ${JENKINS_WEB}" >&2
  exit 1
fi

if [ ! -f build.gradle ]; then
  echo "Not being run from repository root." 1>&2
  exit 1
fi

#set up Jenkins env vars
export JENKINS_HEADERS_FILE=$(mktemp)
source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh

#upgrade jenkins.war and all plugins
if [ -z "${NO_UPGRADE}" ]; then
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/upgradeJenkinsAndPlugins.groovy || (
    echo "Upgrading Jenkins and plugins failed.  Likely, jenkins.war or plugins"
    echo "are not writeable.  This is typical if upgrading jenkins.war via"
    echo "vagrant because jenkins.war is owned by root"
    exit 1
  ) >&2

  echo -n 'Waiting for Jenkins and plugins to be downloaded.'
  while [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/isUpgradeInProgress.groovy)" = 'true' ]; do
    sleep 3
    echo -n '.'
  done
  echo
fi

if [ -z "${NO_UPGRADE}" -a "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/needsRestart.groovy)" = 'true' ]; then
  #restart Jenkins
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url <(echo "Jenkins.instance.restart()")
  #wait for jenkins to become available
  "${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

  #set up Jenkins env vars post-restart
  source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
fi

if grep -- '^version' gradle.properties; then
  export USE_GRADLE_PROPERTIES=1
fi

function get_version() {
  awk 'BEGIN { FS="=" }; $1 == "version" { print $2 }' < gradle.properties
}

if [ -n "${USE_GRADLE_PROPERTIES:-}" ]; then
  VERSION="$(get_version)"
  # increment the patch
  VERSION="${VERSION%.*}.$(( ${VERSION##*.} + 1 ))"
fi

#write jenkins master and plugin versions to build.gradle and dependencies.gradle
"${SCRIPT_LIBRARY_PATH}"/upgrade/remote_dependencies.sh

# GNU or BSD sed is required.  Homebrew on Mac installs GNU sed as gsed.
# Try to detect gsed; otherwise, fall back to detecting OS for using sed.
SED=()
if type -P gsed &> /dev/null; then
  SED=(gsed -r)
elif [ "$(uname -s)" = "Darwin" ] || uname -s | grep -- 'BSD$' &> /dev/null; then
  SED=(sed -E)
else
  # assume Linux GNU sed
  SED=(sed -r)
fi

if [ -n "${USE_GRADLE_PROPERTIES:-}" ]; then
  NEW_VERSION="$(get_version)"
  if [ "${NEW_VERSION%.*}" = "${VERSION%.*}" ]; then
    "${SED[@]}" -i.bak -- "s/^(version)=.*/\\1=${VERSION}/" gradle.properties
    rm gradle.properties.bak
  fi
fi
