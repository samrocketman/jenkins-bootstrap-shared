#!/bin/bash

if [ -z "${JENKINS_WEB}" -o -z "${SCRIPT_LIBRARY_PATH}" ]; then
  echo 'ERROR: required variables are empty and should not be.' >&2
  echo "JENKINS_WEB = ${JENKINS_WEB}" >&2
  echo "SCRIPT_LIBRARY_PATH = ${SCRIPT_LIBRARY_PATH}" >&2
  exit 1
fi

#GNU sed is required.  Homebrew on Mac installs GNU sed as gsed.
#Try to detect gsed; otherwise, fall back to just using sed.
[ -x "$(type -P gsed)" ] && SED=gsed || SED=sed
export SED


function cleanup_temp_on() {
  [ -z "${TMP_DIR}" ] || rm -rf "${TMP_DIR}"
}
trap cleanup_temp_on EXIT

#temp files will automatically be cleaned up on exit because of trap function
TMP_DIR=$(mktemp -d)
TMPFILE="${TMP_DIR}/tmp"
GAV_TMPFILE="${TMP_DIR}/gav"
CUSTOM_TMPFILE="${TMP_DIR}/custom"

echo 'Upgrade build.gradle file.'
export JENKINS_CALL_ARGS="-m POST ${JENKINS_WEB}/scriptText --data-string script= -d"
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/generateSedExpr.groovy > "${TMPFILE}"
$SED -i.bak -rf "${TMPFILE}" build.gradle
rm build.gradle.bak

#generate a new dependencies.gradle file
echo 'Upgrade dependencies.gradle file.'
if [ ! "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url <(echo 'println Jenkins.instance.pluginManager.plugins.size()'))" = '0' ]; then
  #create an index of installed plugins
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/listShortNameVersion.groovy > "${TMPFILE}"

  JENKINS_WAR_VERSION=$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url <(echo 'println Jenkins.instance.version'))
  cat > dependencies.gradle <<-EOF
dependencies {
    //get Jenkins
    getjenkins 'org.jenkins-ci.main:jenkins-war:${JENKINS_WAR_VERSION}@war'

    //custom plugins (if any) provided by custom-plugins.txt; format: G:A:V@hpi or G:A:V@jpi
EOF

  #load custom plugins if a user has defined custom-plugins.txt
  if [ -f custom-plugins.txt ]; then
    #get a list of the custom entries
    gawk '$0 ~ /^([-.a-zA-Z0-9]+:){2}[-.a-zA-Z0-9]+@[hj]pi$/ { print $0 }' custom-plugins.txt | while read gav; do
      echo "    getplugins '${gav}'"
    done >> dependencies.gradle
    #get a list of the plugin IDs only
    gawk 'BEGIN { FS=":" }; $0 ~ /^([-.a-zA-Z0-9]+:){2}[-.a-zA-Z0-9]+@[hj]pi$/ { print $2 }' custom-plugins.txt > "${CUSTOM_TMPFILE}"
  fi
  touch "${CUSTOM_TMPFILE}"

  cat >> dependencies.gradle <<-EOF

    //get plugins
EOF

  while read x; do
    if grep -F "${x%:*}" "${CUSTOM_TMPFILE}" > /dev/null; then
      #skip custom plugins because they're already included in dependencies.gradle
      continue
    fi
    [ -f "${GAV_TMPFILE}" ] || "${SCRIPT_LIBRARY_PATH}"/upgrade/plugins_gav.sh > "${GAV_TMPFILE}"
    GROUP=$(gawk "BEGIN {FS=\":\"};\$2 == \"${x%:*}\" { print \$1 }" "${GAV_TMPFILE}")
    echo "    getplugins '${GROUP}:${x}@hpi'"
    unset GROUP
  done < "${TMPFILE}" | LC_COLLATE=C sort >> dependencies.gradle
  echo '}' >> dependencies.gradle
else
  $SED -i.bak -rf "${TMPFILE}" dependencies.gradle
  rm dependencies.gradle.bak
fi
echo 'Done.'
