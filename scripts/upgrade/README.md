# Upgrading

The following descripts how to upgrade the version of Jenkins and plugins
referenced in this repository.  Both Jenkins core and plugins versions are
pinned within [`build.gradle`](../../build.gradle).  This process will outline
how to painlessly upgrade the versions.

### Prerequisites

Download and add to your `$PATH` [`jenkins-call-url`][1].

### Upgrade Jenkins and plugins

From the root of the repository run:

    ./scripts/upgrade/upgrade_build_gradle.sh

It executes the [`upgrade_build_gradle.sh`](upgrade_build_gradle.sh) script.  It
will automatically log into Jenkins, download core, upgrade all plugins, restart
Jenkins, and edit the `build.gradle` file with the latest revisions.  It is able
to accomplish this by executing scripts within the [Jenkins script console][2].

### Add missing plugins

After running `upgrade_build_gradle.sh`, if there are missing plugins then add
it to `build.gradle` in the `getplugins` section referring to
[`repo.jenkins-ci.org][3] for [GAV coordinates][4] of the missing plugin.

### Run through tests

After upgrading purge and bootstrap Jenkins again.  Run through the test
sequence documented in [root README](../../README.md).

[1]: https://github.com/samrocketman/home/blob/d88bbc190f6d4abad9157aacd614cae38b2e8254/bin/jenkins-call-url
[2]: https://wiki.jenkins-ci.org/display/JENKINS/Jenkins+Script+Console
[3]: https://repo.jenkins-ci.org/
[4]: https://maven.apache.org/pom.html#Maven_Coordinates
