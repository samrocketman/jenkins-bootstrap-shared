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
  [ -n "${TMPFILE}" ] && rm -f "${TMPFILE}"
  [ -n "${GAV_TMPFILE}" ] && rm -f "${GAV_TMPFILE}"
}
trap cleanup_temp_on EXIT

#temp file will automatically be cleaned up on exit because of trap function
TMPFILE=$(mktemp)
GAV_TMPFILE=$(mktemp)
rm -f "${GAV_TMPFILE}"

echo 'Upgrade build.gradle file.'
export JENKINS_CALL_ARGS="-m POST ${JENKINS_WEB}/scriptText --data-string script= -d"
"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/generateSedExpr.groovy > "${TMPFILE}"
$SED -i.bak -rf "${TMPFILE}" build.gradle
rm build.gradle.bak

#generate a new dependencies.gradle file
echo 'Upgrade dependencies.gradle file.'
if [ -d 'jenkins-bootstrap-shared' ]; then
  #create an index of installed plugins
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "${SCRIPT_LIBRARY_PATH}"/upgrade/listShortNameVersion.groovy > "${TMPFILE}"

  JENKINS_WAR_VERSION=$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url <(echo 'println Jenkins.instance.version'))
cat > dependencies.gradle <<-EOF
dependencies {
    //get Jenkins
    getjenkins 'org.jenkins-ci.main:jenkins-war:${JENKINS_WAR_VERSION}@war'

    //get plugins
EOF

  while read x; do
      [ -f "${GAV_TMPFILE}" ] || "${SCRIPT_LIBRARY_PATH}"/upgrade/plugins_gav.sh > "${GAV_TMPFILE}"
      GROUP=$(awk "BEGIN {FS=\":\"};\$2 == \"${x%:*}\" { print \$1 }" "${GAV_TMPFILE}")
      echo "    getplugins '${GROUP}:${x}@hpi'" >> dependencies.gradle
      unset GROUP
  done < "${TMPFILE}"
  echo '}' >> dependencies.gradle
else
  $SED -i.bak -rf "${TMPFILE}" dependencies.gradle
  rm build.gradle.bak
fi
echo 'Done.'
