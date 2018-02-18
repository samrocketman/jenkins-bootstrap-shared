export VAGRANT_SSH_CONFIG=$(mktemp)
vagrant ssh-config > "$VAGRANT_SSH_CONFIG"
while ! ssh -nF "${VAGRANT_SSH_CONFIG}" default 'sudo test -f /var/lib/jenkins/secrets/initialAdminPassword'; do
  echo '/var/lib/jenkins/secrets/initialAdminPassword not available, yet...'
  sleep 5
done
export JENKINS_PASSWORD=$(ssh -F "${VAGRANT_SSH_CONFIG}" default 'sudo cat /var/lib/jenkins/secrets/initialAdminPassword')
