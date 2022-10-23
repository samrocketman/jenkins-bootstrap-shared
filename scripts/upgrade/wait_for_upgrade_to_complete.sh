#!/bin/bash
#Created by Sam Gleske
#Sat 19 Feb 2022 03:11:54 PM EST
#Ubuntu 20.04.3 LTS
#Linux 5.13.0-30-generic x86_64
#GNU bash, version 5.0.17(1)-release (x86_64-pc-linux-gnu)

# Waits for an upgrade to complete
echo -n 'Waiting for Jenkins upgrade to complete.'
while [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh "${SCRIPT_LIBRARY_PATH}"/upgrade/isUpgradeInProgress.groovy)" = 'true' ]; do
  sleep 3
  echo -n '.'
done
echo
