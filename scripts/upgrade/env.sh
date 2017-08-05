
function canonicalize() (
  cd "${1%/*}"
  echo "${PWD}/${1##*/}"
)

if [ -f build.gradle ]; then
  export JENKINS_USER=admin
  export JENKINS_PASSWORD="$(<"${JENKINS_HOME}"/secrets/initialAdminPassword)"
  export JENKINS_HEADERS_FILE=$(canonicalize "${JENKINS_HOME}"/secrets/.http-headers.json)
  unset JENKINS_CALL_ARGS
  jenkins-call-url -a -v -v "${JENKINS_WEB}"/api/json -o /dev/null
  export JENKINS_CALL_ARGS="-m POST -v ${JENKINS_WEB}/scriptText --data-string script= -d"
else
  echo "Not in repository root." 1>&2
fi
