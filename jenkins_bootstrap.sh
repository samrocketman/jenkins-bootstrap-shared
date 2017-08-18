#!/bin/bash
#Created by Sam Gleske (https://github.com/samrocketman)
#Wed May 20 23:22:07 EDT 2015
#Ubuntu 14.04.2 LTS
#Linux 3.13.0-52-generic x86_64
#GNU bash, version 4.3.11(1)-release (x86_64-pc-linux-gnu)
#curl 7.35.0 (x86_64-pc-linux-gnu) libcurl/7.35.0 OpenSSL/1.0.1f zlib/1.2.8 libidn/1.28 librtmp/2.3
#Source: https://github.com/samrocketman/jenkins-bootstrap-jervis

#A script which bootstraps a Jenkins installation for executing Jervis Job DSL
#scripts

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

source env.sh
#set password if using vagrant
[ -n "${VAGRANT_JENKINS}" ] && source "${SCRIPT_LIBRARY_PATH}/vagrant-env.sh"
export JENKINS_HEADERS_FILE="$(mktemp)"
export JENKINS_USER="admin"

set -e


if [ -e "${SCRIPT_LIBRARY_PATH}/common.sh" ]; then
  source "${SCRIPT_LIBRARY_PATH}/common.sh"
else
  echo "ERROR could not find ${SCRIPT_LIBRARY_PATH}/common.sh"
  echo "Perhaps environment variable SCRIPT_LIBRARY_PATH is not set correctly."
  exit 1
fi

#provision jenkins and plugins
if [ -z "${REMOTE_JENKINS}" -a -z "${VAGRANT_JENKINS}" ]; then
  echo 'Downloading specific versions of Jenkins and plugins...'
  ./gradlew getjenkins getplugins
fi

if [ -d "./plugins" ]; then
  mkdir -p "${JENKINS_HOME}/plugins"
  ( cd "./plugins/"; ls -1d * ) | while read x; do
    if [ ! -e "${JENKINS_HOME}/plugins/${x}" ]; then
      echo "Copying ${x} to JENKINS_HOME"
      cp -r "./plugins/${x}" "${JENKINS_HOME}/plugins/"
      #pin plugin versions
      #https://wiki.jenkins-ci.org/display/JENKINS/Pinned+Plugins
      touch "${JENKINS_HOME}/plugins/${x}.pinned"
    fi
  done
fi

#download jenkins, start it up, and update the plugins
if [ -z "${REMOTE_JENKINS}" -a -z "${VAGRANT_JENKINS}" ]; then
  if [ ! -e "${JENKINS_WAR}" ]; then
    "${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" download-file "${jenkins_url}" "${JENKINS_WAR}"
  fi
  #check for running jenkins or try to start it
  if ! "${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" status; then
    "${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" start
  fi
fi

#wait for jenkins to become available
"${SCRIPT_LIBRARY_PATH}/provision_jenkins.sh" url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

#persist credentials
export JENKINS_PASSWORD="${JENKINS_PASSWORD:-$(<"${JENKINS_HOME}"/secrets/initialAdminPassword)}"
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url -a -m HEAD -o /dev/null ${JENKINS_WEB}

if grep -- '^ \+getplugins' dependencies.gradle > /dev/null; then
  jenkins_console --script "${SCRIPT_LIBRARY_PATH}/console-skip-2.0-wizard.groovy"
fi
jenkins_console --script "${SCRIPT_LIBRARY_PATH}/configure-disable-usage-stats.groovy"

#configure jenkins agent credentials
#jenkins_console --script "${SCRIPT_LIBRARY_PATH}/credentials-jenkins-agent.groovy"
#disable agent -> master security
#jenkins_console --script "${SCRIPT_LIBRARY_PATH}/security-disable-agent-master.groovy"
