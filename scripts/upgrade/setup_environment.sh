#Created by Sam Gleske
#Sat 19 Feb 2022 03:28:16 PM EST
#Ubuntu 20.04.3 LTS
#Linux 5.13.0-30-generic x86_64
#GNU bash, version 5.0.17(1)-release (x86_64-pc-linux-gnu)

# DESCRIPTION
#     This script is meant to be sourced.  It runs some pre-upgrade checks.

#set password if using vagrant
[ -n "${VAGRANT_JENKINS}" ] && source "${SCRIPT_LIBRARY_PATH}/vagrant-env.sh"
[ -n "${DOCKER_JENKINS}" ] && source "${SCRIPT_LIBRARY_PATH}/docker-env.sh"


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
export JENKINS_HEADERS_FILE=$(mktemp)
source "${SCRIPT_LIBRARY_PATH}"/upgrade/env.sh
