# Migration notes Oct 2022

A lot is changing this update over prior jenkins-bootstrap-shared upgrades.
This document attempts to provide a short list of changes required of end users
if they're updating.

This update includes dependency updates because a lot has changed in upstream
Jenkins.  This update also contains terminology updates to match Jenkins
community guidelines.

- [Oracle JDK 8 dropped](#oracle-jdk-8-dropped)
- [Remove configuration scripts](#remove-configuration-scripts)
- [Some configuration scripts have been renamed](#some-configuration-scripts-have-been-renamed)
- [Settings for scripts have changed](#settings-for-scripts-have-changed)
  - [configure-docker-cloud.groovy](#configure-docker-cloudgroovy)
  - [configure-jenkins-settings.groovy](#configure-jenkins-settingsgroovy)
  - [configure-yadocker-cloud.groovy](#configure-yadocker-cloudgroovy)

# Before and after migration

You should capture plugin names from `dependencies.gradle`.  Compare differences
before and after upgrade in order to avoid potential issues.

    ./scripts/upgrade/show_plugin_artifacts.sh > ../before-upgrade

Run through upgrade following [upgrade README][1].

    ./scripts/upgrade/show_plugin_artifacts.sh > ../after-upgrade

Compare the differences.

    diff -u ../before-upgrade ../after-upgrade

[1]: scripts/upgrade/README.md


# Oracle JDK 8 dropped

Oracle JDK 8 has been completely dropped.  OpenJDK 11 is now required.  If
upgrading from older version of Jervis use the older Docker image which contains
Oracle JDK 8.  After update start with the latest Docker image because it
contains OpenJDK 11.

# Remove configuration scripts

`security-disable-agent-master.groovy` is no longer supported in newer
versions of Jenkins.  Remove refrences from `jenkins_bootstrap.sh`.

It has been renamed for future archival.

    security-disable-agent-master.groovy -> security-disable-agent-controller.groovy

# Some configuration scripts have been renamed

`scripts/` directory files have been renamed.  Users will need to update
`jenkins_bootstrap.sh` to reflect the following.

    configure-job-restrictions-master.groovy -> configure-job-restrictions-controller.groovy

# Settings for scripts have changed

Some configuration files have had their settings updated.

### configure-docker-cloud.groovy

Settings containing `slave` have been renamed to use `agent.

    launcher_prefix_start_slave_cmd -> launcher_prefix_start_agent_cmd
    launcher_suffix_start_slave_cmd -> launcher_suffix_start_agent_cmd

### configure-jenkins-settings.groovy

The variable used for customizing settings has changed.  In your configs change
the following.

    master_settings -> controller_settings

Settings containing `master` have been renamed to use `controller`.

    master_executors -> controller_executors
    master_labels -> controller_labels
    master_usage -> controller_usage

Settings containing `slave` have been renamed to use `agent.

    jnlp_slave_port -> jnlp_agent_port

### configure-yadocker-cloud.groovy

Settings containing `slave` have been renamed to use `agent.

    launch_ssh_prefix_start_slave_command -> launch_ssh_prefix_start_agent_command
    launch_ssh_suffix_start_slave_command -> launch_ssh_suffix_start_agent_command
    launch_jnlp_slave_jar_options -> launch_jnlp_agent_jar_options
    launch_jnlp_slave_jvm_options -> launch_jnlp_agent_jvm_options

Settings containing `master` have been renamed to use `controller`.

    launch_jnlp_different_jenkins_master_url -> launch_jnlp_different_jenkins_controller_url
