
function canonicalize() (
  cd "${1%/*}"
  echo "${PWD}/${1##*/}"
)

if [ -n "$(type -P jenkins-call-url)" ]; then
  if [ -f build.gradle ]; then
    export JENKINS_USER=admin
    export JENKINS_PASSWORD="$(<../my_jenkins_home/secrets/initialAdminPassword)"
    export JENKINS_HEADERS_FILE=$(canonicalize ../my_jenkins_home/secrets/.http-headers.json)
    unset JENKINS_CALL_ARGS
    jenkins-call-url -a -v -v http://localhost:8080/api/json -o /dev/null
    export JENKINS_CALL_ARGS='-m POST -v http://localhost:8080/scriptText --data-string script= -d'
  else
    echo "Not in repository root." 1>&2
  fi
else
  echo "Missing jenkins-call-url script." 1>&2
fi
