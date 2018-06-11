export VAGRANT_SSH_CONFIG=$(mktemp)
vagrant ssh-config > "$VAGRANT_SSH_CONFIG"
function get_jenkins_home() {
  ssh -F "${VAGRANT_SSH_CONFIG}" default /bin/bash <<'EOF'
CONFIG_FILE=""
if sudo test -f /etc/sysconfig/jenkins && sudo grep '^JENKINS_HOME' /etc/sysconfig/jenkins &> /dev/null; then
  CONFIG_FILE='/etc/sysconfig/jenkins'
elif sudo test -f /etc/default/jenkins && sudo grep '^JENKINS_HOME' /etc/default/jenkins &> /dev/null; then
  CONFIG_FILE='/etc/default/jenkins'
fi
if test -n "${CONFIG_FILE}"; then
  eval "$(sudo grep '^JENKINS_HOME' "${CONFIG_FILE}")"
fi
echo "${JENKINS_HOME:-/var/lib/jenkins}"
EOF
}

VAGRANT_JENKINS_HOME="$(get_jenkins_home)"

if [ -z "${JENKINS_PASSWORD}" ]; then
  while ! ssh -nF "${VAGRANT_SSH_CONFIG}" default "sudo test -f \"${VAGRANT_JENKINS_HOME}\"/secrets/initialAdminPassword"; do
    echo '/var/lib/jenkins/secrets/initialAdminPassword not available, yet...'
    sleep 5
  done
  export JENKINS_PASSWORD=$(ssh -F "${VAGRANT_SSH_CONFIG}" default "sudo cat \"${VAGRANT_JENKINS_HOME}\"/secrets/initialAdminPassword")
fi
