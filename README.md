# jenkins-bootstrap shared scripts [![Build Status][ci-img]][ci-link]

<img
src="https://user-images.githubusercontent.com/875669/35621130-2acb1e78-0638-11e8-8777-0f56edc79c32.png"
height=48 width=48 alt="Jenkins CI logo" /> <img
src="https://user-images.githubusercontent.com/875669/35621286-b3d3b9b4-0638-11e8-956c-169993f8042e.png"
height=48 width=48 alt="Ubuntu Logo" /> <img
src="https://user-images.githubusercontent.com/875669/35621322-cf8ec752-0638-11e8-8dbc-72760b696d64.png"
height=48 width=48 alt="Red Hat Logo" /> <img
src="https://user-images.githubusercontent.com/875669/35621353-e78a6956-0638-11e8-8e07-3d96e9e91dd7.png"
height=48 width=72 alt="Docker Logo" /> <img
src="https://user-images.githubusercontent.com/875669/35621372-f72f6d16-0638-11e8-93d6-2ae335fc2382.png"
height=48 width=48 alt="Vagrant Logo" />

- Jenkins as immutable infrastructure made easy.

Jenkins is traditionally challenging to safely QA and test upgrades.  This
project aims to make safely managing a Jenkins instance and all of its plugins
easy.

Goals of this project:

- Keep the bulk of the logic in a shared project (this one).
- Allow other projects to source this project making changes easy to read in
  downstream projects.
- Provide a standard bootstrapper across all downstream projects.

# Requirements

- Mac OS X or Linux.
- More than two CPU cores recommended.
- More than 6GB of RAM recommended if running Jenkins.

Optional requirements for other types of provisioning:

- [Docker][docker]
- [Vagrant][vagrant] + [VirtualBox][vbox]

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
   git add -A && git commit -m 'initial commit'
   ```

2. Bootstrap your new Jenkins version locally.

   ```
   ./jenkins_bootstrap.sh
   ```

3. Visit `http://localhost:8080/` and install all desired plugins.  Be sure to
   continue as admin.

4. Save your Jenkins version and plugins to your new repository.

   ```
   ./jenkins-bootstrap-shared/scripts/upgrade/upgrade_build_gradle.sh
   git add -A && git commit -m 'plugins are installed'
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
   git add -A && git commit -m 'initial commit'
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
   git add -A && git commit -m 'plugins are installed'
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
- `buildTar` - builds a TAR file which is used to build a docker container.
- `packages` - executes `buildRpm`, `buildDeb`, and `buildTar` tasks.
- `getjenkins` - Downloads `jenkins.war` to the current directory.
- `getplugins` - Downloads Jenkins plugin HPI files to `./plugins`.


# Additional Instructions

### Provision Jenkins via Vagrant

This repository optionally uses [Vagrant][vagrant].  To bootstrap Jenkins simply
run the following to start Jenkins.

    vagrant up
    export VAGRANT_JENKINS=1
    ./jenkins_bootstrap.sh

Visit `http://localhost:8080/` to see Jenkins running.

### Provision Jenkins via docker-compose

Bootstrapping Jenkins while using docker-compose is similar to bootstrapping
with Vagrant.

    docker-compose up -d
    export DOCKER_JENKINS=1
    ./jenkins_bootstrap.sh

### Upgrade Jenkins and plugins

To upgrade Jenkins master and plugin versions do the following:

    ./jenkins_bootstrap.sh
    ./scripts/upgrade/upgrade_build_gradle.sh
    git add -A && git commit -m 'jenkins upgraded'

### Build an RPM package

    ./gradlew clean buildRpm

### Build a docker image

**Why not the official image?** Using this docker image has a few advantages
over the official image:

- This image is minimal (~292MB) vs official (~809MB).  Based on Alpine Linux.
- Dependencies during the build when including plugins can be cached in
  Artifactory or Nexus
- More options are exposed while some defaults are sane for running within
  Docker.

**Build it:**

    ./gradlew clean buildTar
    docker build -t jenkins .

Alternatively, if you're building from a downstream project:

    docker build -f jenkins-bootstrap-shared/Dockerfile -t jenkins .

The following environment variables can be overridden in the docker container
if using docker-compose.

| VARIABLE                  | TYPE | DESCRIPTION |
| ------------------------- | ---- | ----------- |
| `JENKINS_HOME`            | Path | Jenkins configuration location. Default is `/var/lib/jenkins`. |
| `JENKINS_PORT`            | int  | set the http listening port. -1 to disable, Default is 8080 |
| `JENKINS_DEBUG_LEVEL`     | int  | set the level of debug msgs (1-9). Default is 5 (INFO level) |
| `JENKINS_HANDLER_MAX`     | int  | set the max no of worker threads to allow. Default is 100 |
| `JENKINS_HANDLER_IDLE`    | int  | set the max no of idle worker threads to allow. Default is 20 |
| `JENKINS_ARGS`            | Str  | Any additional args available to `jenkins.war` |

Docker environment variables related to HTTPS.  Note: HTTPS will only be
available if a keystore is defined.  All other variables are disabled without
it.

| VARIABLE                               | TYPE | DESCRIPTION |
| -------------------------------------- | ---- | ----------- |
| `JENKINS_KEYSTORE`                     | Path | the location of the SSL KeyStore file. |
| `JENKINS_HTTPS_PORT`                   | int  | set the https listening port. -1 to disable, Default is 8443. |
| `JENKINS_HTTPS_KEYSTORE_PASSWORD`      | Str  | the password for the SSL KeyStore file. Default is changeit |
| `JENKINS_HTTPS_KEYSTORE_PASSWORD_FILE` | Path | Same as password but in a file. |

> Note: if you plan to start the docker container from an existing Jenkins home,
> you must first set permissions to the uid/gid of the jenkins user inside the
> container.  Example:
>
>     $ docker run -it --rm jenkinsbootstrapshared_jenkins id
>     uid=100(jenkins) gid=65533(nogroup) groups=65533(nogroup)
>
>     $ chown -R 100:65533 /path/to/jenkins/home

### Other Usage

For service control and other usage see [`USAGE`](USAGE.md).

# License

* [ASL 2](LICENSE)
* [3rd party licenses](3rd_party)

[ci-img]: https://travis-ci.org/samrocketman/jenkins-bootstrap-shared.svg?branch=master
[ci-link]: https://travis-ci.org/samrocketman/jenkins-bootstrap-shared
[docker]: https://www.docker.com/
[vagrant]: https://www.vagrantup.com/
[vbox]: https://www.virtualbox.org/
