# Upgrading

The following descripts how to upgrade the version of Jenkins and plugins
referenced in this repository.  Both Jenkins core and plugins versions are
pinned within [`build.gradle`](../../build.gradle).  This process will outline
how to painlessly upgrade the versions.

### Upgrade Jenkins and plugins

From the root of the repository run:

    ./scripts/upgrade/upgrade_build_gradle.sh

It executes the [`upgrade_build_gradle.sh`](upgrade_build_gradle.sh) script.  It
will automatically log into Jenkins, download core, upgrade all plugins, restart
Jenkins, and edit the `build.gradle` file with the latest revisions.  It is able
to accomplish this by executing scripts within the [Jenkins script console][1].


### What happens?

Plugins are stored at [`repo.jenkins-ci.org`][2].  Gradle is used to download
plugins using [GAV coordinates][3].  When running `upgrade_build_gradle.sh`,
a local Jenkins instance is upgraded and all of the plugins are upgraded.  Once
Jenkins is upgraded then `build.gradle` and `dependencies.gradle` is updated
with the new Jenkins master and plugin versions.

### Override Jenkins Update Center

During the upgrade process it might be desirable to use a different Jenkins
Update Center (e.g. the [experimental update center][4]).

    export JENKINS_UPDATE_CENTER='https://updates.jenkins.io/experimental/update-center.json'
    ./scripts/upgrade/upgrade_build_gradle.sh

[1]: https://wiki.jenkins-ci.org/display/JENKINS/Jenkins+Script+Console
[2]: https://repo.jenkins-ci.org/
[3]: https://maven.apache.org/pom.html#Maven_Coordinates
[4]: https://jenkins.io/doc/developer/publishing/releasing-experimental-updates/
