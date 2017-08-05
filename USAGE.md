# Usage Instructions

### Provision Jenkins

To provision Jenkins locally outside of a vagrant VM.

    ./jenkins_bootstrap.sh

Visit `http://localhost:8080/` to see Jenkins running.

### Control Jenkins

The [`provision_jenkins.sh`](scripts/provision_jenkins.sh) script is what
controls Jenkins running on localhost.  It has a few helper commands associated
with it.  Before running any commands it is recommended to add the `scripts`
directory to your local `PATH` environment variable.

    PATH="./scripts:$PATH"
    unset JENKINS_HOME

Now control the Jenkins service.

    provision_jenkins.sh stop
    provision_jenkins.sh start
    provision_jenkins.sh restart


See other options.

    provision_jenkins.sh --help

### Delete Jenkins

Clean up everything:

    ./gradlew clean

# Upgrading Jenkins and plugins

Jenkins is a fast moving development target.  In order to ensure this repository
stays up to date with development in Jenkins these instructions are provided.

To upgrade the stable Jenkins and plugins versions used for testing PRs follow
the [upgrade instructions](scripts/upgrade/README.md).
