#!/bin/bash
#Created by Sam Gleske
#Sat 19 Feb 2022 03:14:39 PM EST
#Ubuntu 20.04.3 LTS
#Linux 5.13.0-30-generic x86_64
#GNU bash, version 5.0.17(1)-release (x86_64-pc-linux-gnu)

# DESCRIPTION
#     If Jenkins needs a restart, then restart
#
# EXIT CODES
#     Returns
#       0 - If Jenkins restarts.  Zero exit code.
#       1 - If Jenkins does not need to restart.  Non-zero exit code.

EXIT_CODE=1
if [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh "${SCRIPT_LIBRARY_PATH}"/upgrade/needsRestart.groovy)" = 'true' ]; then
  #restart Jenkins
  "${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh <(echo "Jenkins.instance.restart()")
  #wait for jenkins to become available
  "${SCRIPT_LIBRARY_PATH}"/provision_jenkins.sh url-ready "${JENKINS_WEB}/jnlpJars/jenkins-cli.jar"
  EXIT_CODE=0
fi

exit "${EXIT_CODE}"
