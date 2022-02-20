# Plugin refresh procedure

If you haven't already create a `pinned-plugins.txt` file at the root of your
jenkins bootstrap repository.

1. Bootstrap Jenkins normally and capture a list of plugins.
2. Bootstrap Jenkins without plugins.
3. Run the refresh plugins script which will install the plugins and update your
   gradle files.
4. Capture list of plugins again to compare before-after.

# Procedure

### 1. Capture list of plugins

> **Please note:** The purpose of capturing a list of plugins before and after
> upgrade is to compare what plugins were added and removed.  Inspect the list
> and ensure a plugin in which you depend upon is not removed.  Any missing
> plugins identified should be added to `pinned-plugins.txt` and restart the
> procedure to refresh plugins.

Bootstrap your Jenkins instance normally with the following command.

    ./jenkins_bootstrap.sh

You can capture a full list of plugins before and after plugin refresh to ensure
no important plugins are dropped.  The following is a [script
console][script-console] script.

```groovy
println Jenkins.instance.pluginManager.plugins*.shortName.sort().unique().join('\n')
```

The above script is run from the script console.  Save the result of this script
to a file called `old_plugins.txt`.

### 2. Bootstrap Jenkins without plugins

A new option is provided if you upgrade your jenkins-bootstrap-shared submodule.
`--skip-plugins`.  Run the following command to bootstrap your version of
Jenkins without plugins.

    ./jenkins_boostrap.sh --skip-plugins

### 3. Run refresh plugins script

From your repository initialized with jenkins-bootstrap-shared run the following
command.

    ./jenkins-bootstrap-shared/scripts/upgrade/refresh_plugins.sh

It will upgrade your Jenkins instance and reboot.  After reboot it will install
all plugins listed in `pinned-plugins.txt`.  Finally, it will call
[`upgrade_build_gradle.sh`](upgrade_build_gradle.sh) script which will update
your `dependencies.gradle` and `gradle.properties` file with the new plugin
versions.

### 4. Capture a list of plugins to compare

Re-run the script console script listed in section 1 and save the list of
plugins to `new_plugins.txt`.  You can compare a unified diff between plugins
which have been added or removed.

    git diff --color -u old_plugins.txt new_plugins.txt

If you determine you're missing critical plugins, then add the missing plugin
IDs to `pinned-plugins.txt` and restart the plugin refresh procedure.

[script-console]: https://www.jenkins.io/doc/book/managing/script-console/
