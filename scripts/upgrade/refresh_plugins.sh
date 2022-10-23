#!/bin/bash
#Created by Sam Gleske
#Sat 19 Feb 2022 02:28:59 PM EST
#Ubuntu 20.04.3 LTS
#Linux 5.13.0-30-generic x86_64
#GNU bash, version 5.0.17(1)-release (x86_64-pc-linux-gnu)

set -e

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
source "${SCRIPT_LIBRARY_PATH}"/upgrade/setup_environment.sh

#wait for jenkins to become available
"${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"

if [ ! -f pinned-plugins.txt ]; then
  echo 'ERROR: No pinned plugins found so cannot proceed.' >&2
  exit 1
fi

"${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh "${SCRIPT_LIBRARY_PATH}"/upgrade/upgradeJenkinsOnly.groovy || (
  echo "Upgrading Jenkins and plugins failed.  Likely, jenkins.war or plugins"
  echo "are not writeable.  This is typical if upgrading jenkins.war via"
  echo "vagrant because jenkins.war is owned by root"
  exit 1
) >&2

# upgrade and restart if necessary
"${SCRIPT_LIBRARY_PATH}"/upgrade/wait_for_upgrade_to_complete.sh
if "${SCRIPT_LIBRARY_PATH}"/upgrade/jenkins_needs_restart.sh; then
  #set up Jenkins env vars post-restart
  source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
fi

"${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh "${SCRIPT_LIBRARY_PATH}"/upgrade/installMinimalPlugins.groovy --data-string "plugins='''$(< pinned-plugins.txt)'''.trim();"
"${SCRIPT_LIBRARY_PATH}"/upgrade/wait_for_upgrade_to_complete.sh
if "${SCRIPT_LIBRARY_PATH}"/upgrade/jenkins_needs_restart.sh; then
  #set up Jenkins env vars post-restart
  source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
fi
export NO_UPGRADE=1
"${SCRIPT_LIBRARY_PATH}"/upgrade/upgrade_build_gradle.sh*
