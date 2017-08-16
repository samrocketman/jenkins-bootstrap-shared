export VAGRANT_SSH_CONFIG=$(mktemp)
vagrant ssh-config > "$VAGRANT_SSH_CONFIG"
export JENKINS_PASSWORD=$(ssh -F "${VAGRANT_SSH_CONFIG}" default 'sudo cat /var/lib/jenkins/secrets/initialAdminPassword')
