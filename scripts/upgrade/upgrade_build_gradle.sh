#!/bin/bash
#Created by Sam Gleske
#Fri Apr 21 23:26:16 PDT 2017
#Ubuntu 16.04.2 LTS
#Linux 4.4.0-72-generic x86_64

set -e

function cleanup_on() {
  [ -n "${TMPFILE}" ] && rm -f "${TMPFILE}"
  [ -n "${GAV_TMPFILE}" ] && rm -f "${GAV_TMPFILE}"
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

source env.sh
source "${SCRIPT_LIBRARY_PATH}"/upgrade/setup_environment.sh

#allow users to import when using no upgrade
[ -n "${NO_UPGRADE}" ] && FORCE_UPGRADE=1

#wait for jenkins to become available
"${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

#upgrade jenkins.war and all plugins
if [ -z "${NO_UPGRADE}" ]; then
  "${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh "${SCRIPT_LIBRARY_PATH}"/upgrade/upgradeJenkinsAndPlugins.groovy || (
    echo "Upgrading Jenkins and plugins failed.  Likely, jenkins.war or plugins"
    echo "are not writeable.  This is typical if upgrading jenkins.war via"
    echo "vagrant because jenkins.war is owned by root"
    exit 1
  ) >&2

  "${SCRIPT_LIBRARY_PATH}"/upgrade/wait_for_upgrade_to_complete.sh
  if "${SCRIPT_LIBRARY_PATH}"/upgrade/jenkins_needs_restart.sh; then
    #set up Jenkins env vars post-restart
    source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
  fi
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

#write jenkins controller and plugin versions to build.gradle and dependencies.gradle
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
