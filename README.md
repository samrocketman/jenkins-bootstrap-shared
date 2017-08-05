# jenkins-bootstrap shared scripts

Jenkins is traditionally challenging to safely QA and test upgrades.  This
project aims to make managing a Jenkins instance and all of its plugins easy.

Goals of this project:

- Keep the bulk of the logic in a shared project (this one).
- Allow other projects to source this project making changes easy to read in
  downstream projects.
- Provide a standard bootstrapper across all downstream projects.

# Requirements

- Mac OS X or Linux.
- More than two CPU cores recommended.
- More than 6GB of RAM recommended if running Jenkins.

# Getting Started

How to use this shared bootstrapper with your own scripts.

### Creating a new Jenkins instance

If you're installing Jenkins for the first time then start here.  Otherwise,
skip to the next section.

1. Create a new repository.

   ```
   mkdir my-project
   cd my-project
   git init
   git submodule add https://github.com/samrocketman/jenkins-bootstrap-shared
   ./jenkins-bootstrap-shared/init.sh
   git commit -m 'initial commit'
   ```

2. Bootstrap your new Jenkins version locally.

   ```
   ./jenkins_bootstrap.sh
   ```

3. Visit `http://localhost:8080/` and install all desired plugins.

4. Save your Jenkins version and plugins to your new repository.

   ```
   ./jenkins-bootstrap-shared/scripts/upgrade/upgrade_build_gradle.sh
   git commit -m 'plugins are installed'
   ```

### Import an existing Jenkins instance

Often, readers will already have an existing Jenkins instance.  These
instructions allow one to convert an existing instance to using these bootstrap
scripts.  To do this, administer privileges are required on the exiting Jenkins
instance.

1. Create a new repository.

   ```
   mkdir my-project
   cd my-project
   git init
   git submodule add https://github.com/samrocketman/jenkins-bootstrap-shared
   ./jenkins-bootstrap-shared/init.sh
   git commit -m 'initial commit'
   ```

2. Prepare authentication for your remote Jenkins instance.

   ```
   export JENKINS_WEB='https://jenkins.example.com/'
   export JENKINS_HEADERS_FILE=$(mktemp)
   export SCRIPT_LIBRARY_PATH="./jenkins-bootstrap-shared/scripts"
   JENKINS_USER=user JENKINS_PASSWORD=pass ./jenkins-bootstrap-shared/scripts/jenkins-call-url -m HEAD -o /dev/null -a -vv "${JENKINS_WEB}"
   ```

3. Import your remote Jenkins version and plugin versions into this repository.

   ```
   ./jenkins-bootstrap-shared/scripts/upgrade/remote_dependencies.sh
   git commit -m 'plugins are installed'
   ```

4. Clean up environment when finished.

   ```
   rm -f "${JENKINS_HEADERS_FILE}"
   unset JENKINS_WEB SCRIPT_LIBRARY_PATH JENKINS_HEADERS_FILE
   ```

# Next steps

In the root of your new bootstrap repository there is `variables.gradle`.
Customize this to your liking for your setup.  When you're finished I recommend
tagging your repository as a release.

Generate RPM and DEB packages of your Jenkins instance.

    ./gradlew packages

The system packages will be located in `./build/distributions/`.  Your packages
are ready to manage a new Jenkins installation or convert an existing
installation.  These packages can be used to test upgrades before they ever land
in production.

Additionally, this package will track your `$JENKINS_HOME` with `git` during
plugin upgrades and take daily snapshots of your `$JENKINS_HOME`.

# Common gradle tasks

The following tasks would be executed with `./gradlew TASK`.  List of common
TASKs include:

- `clean` - cleans the build directory and all bootstrap related files.
- `buildRpm` - builds an RPM package for RHEL based Linux distros.
- `buildDeb` - builds a DEB package for Debian based Linux distros.
- `packages` - executes both `buildRpm` and `buildDeb` tasks.
- `getjenkins` - Downloads `jenkins.war` to the current directory.
- `getplugins` - Downloads Jenkins plugin HPI files to `./plugins`.

# Upgrade Jenkins and plugins

To upgrade Jenkins master and plugin versions do the following:

    ./jenkins_bootstrap.sh
    ./scripts/upgrade/upgrade_build_gradle.sh
    git commit -m 'jenkins upgraded'

# Instructions

### Provision Jenkins

This repository uses [Vagrant][vagrant].  To bootstrap Jenkins simply run the
following to start Jenkins.

    vagrant up

Visit `http://localhost:8080/` to see Jenkins running.

### Build an RPM package

    ./gradlew clean buildRpm

### Other Usage

For service control and other usage see [`USAGE`](USAGE.md).

# License

* [ASL 2](LICENSE)
* [3rd party licenses](3rd_party)

[vagrant]: https://www.vagrantup.com/
