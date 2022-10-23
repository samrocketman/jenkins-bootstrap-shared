# Plugin Refresh

It is recommended to keep Jenkins lean and up to date that you follow the
[plugin refresh guide](plugin_refresh.md).  Instead of upgrading plugins you
start from scratch with your minimally required plugins each time.  This means
any plugin dependencies dropped from your minimally required plugins will also
be removed.

To use plugin refresh practice the following should be a practice.

- All Jenkins controller configuration is config as code either view [CaC
  plugin][cac] or via jenkins-bootstrap-shared script console scripts.
- All Jenkins jobs should be generated from code with no manually created jobs.

Managing Jenkins without the above conditions is risky because you might be
required to regenerate all of your config from scratch.  This is a typical
scenario if you have long time periods between upgrades and the versions of
plugins installed on your Jenkins controller.  Old Jenkins plugins tend to not
have clean configuration upgrade paths to the latest available Jenkins update
center plugins.

---

# Before and after upgrade

[`show_plugin_artifacts.sh`](show_plugin_artifacts.sh) is meant to capture the
plugin names from your `dependencies.gradle`.  You should call this script
before and after upgrade in order to verify you're not missing necessary
plugins.

Usage

    ./scripts/upgrade/show_plugin_artifacts.sh > ../before-upgrade

    # run the upgrade following instructions below

    ./scripts/upgrade/show_plugin_artifacts.sh > ../after-upgrade

Once you have the two copies you can find the differences.

    diff -u ../before-upgrade ../after-upgrade

This utility is meant to help catch potential upgrade issues.

---

# Plugin Upgrade

> **Please note:** upgrading plugins will keep around old plugins which may no
> longer be dependencies of your minimally required plugins.

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
with the new Jenkins controller and plugin versions.

### Override Jenkins Update Center

During the upgrade process it might be desirable to use a different Jenkins
Update Center (e.g. the [experimental update center][4]).

    export JENKINS_UPDATE_CENTER='https://updates.jenkins.io/experimental/update-center.json'
    ./scripts/upgrade/upgrade_build_gradle.sh

[1]: https://wiki.jenkins-ci.org/display/JENKINS/Jenkins+Script+Console
[2]: https://repo.jenkins-ci.org/
[3]: https://maven.apache.org/pom.html#Maven_Coordinates
[4]: https://jenkins.io/doc/developer/publishing/releasing-experimental-updates/
[cac]: https://plugins.jenkins.io/configuration-as-code/
