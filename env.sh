#sane defaults
export JENKINS_WAR="${JENKINS_WAR:-jenkins.war}"

export JENKINS_HOME="${JENKINS_HOME:-../my_jenkins_home}"
export JENKINS_START="${JENKINS_START:-java -Xms4g -Xmx4g -XX:MaxPermSize=512M -jar ${JENKINS_WAR}}"
export JENKINS_WEB="${JENKINS_WEB:-http://localhost:8080}"
export jenkins_url="${jenkins_url:-http://mirrors.jenkins-ci.org/war/latest/jenkins.war}"

if [ -d 'jenkins-bootstrap-shared' ]; then
  export SCRIPT_LIBRARY_PATH="${SCRIPT_LIBRARY_PATH:-./jenkins-bootstrap-shared/scripts}"
else
  export SCRIPT_LIBRARY_PATH="${SCRIPT_LIBRARY_PATH:-./scripts}"
fi
