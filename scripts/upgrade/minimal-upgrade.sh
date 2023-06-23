#!/bin/bash

set -e

if [ ! -f plugins.txt ]; then
  echo 'ERROR: Missing plugins.txt at the root of your repository with plugin IDs to be installed.' >&2
  exit 1
fi

if ! type -P jq &> /dev/null; then
  echo 'ERROR: missing jq utility in PATH.' >&2
  exit 1
fi

latest_lts() {
  curl -sSfL https://updates.jenkins-ci.org/current/update-center.json | \
  awk 'NR == 1 {next}; {print; exit}' | \
  jq -r '.core.version'
}

# update dependencies.gradle with latest release
sed -i.bak 's/^\( *getjenkins *.*\):[^@]*@\(.*\)/\1:'"$(latest_lts)"'@\2/' \
  dependencies.gradle
rm dependencies.gradle.bak

# Get the boot Jenkins and install plugins using Jenkins CLI
(
  # start Jenkins without plugins
  source env.sh
  "${SCRIPT_LIBRARY_PATH}"/../jenkins_bootstrap.sh --skip-plugins
  source env.sh
  source "${SCRIPT_LIBRARY_PATH}"/upgrade/setup_environment.sh

  # download jenkins cli, install plugins, remove jenkins cli
  curl -sSfLO "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"
  xargs java -jar jenkins-cli.jar \
    -auth "${JENKINS_USER}:${JENKINS_PASSWORD}" \
    -s "${JENKINS_WEB}" \
    -webSocket install-plugin \
    < plugins.txt
  rm jenkins-cli.jar

  "${SCRIPT_LIBRARY_PATH}"/upgrade/wait_for_upgrade_to_complete.sh
  if "${SCRIPT_LIBRARY_PATH}"/upgrade/jenkins_needs_restart.sh; then
    true # do nothing
  fi
)

# update Gradle build files
export NO_UPGRADE=1
source env.sh
"${SCRIPT_LIBRARY_PATH}"/upgrade/upgrade_build_gradle.sh
