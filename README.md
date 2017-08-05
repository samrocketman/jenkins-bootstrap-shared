# jenkins-bootstrap shared scripts

These scripts allow an easily source-able upstream project which allow Jenkins
to be bootstrapped.

# Common gradle tasks

The following tasks would be executed with `./gradlew TASK`.  List of common
TASKs include:

- `clean` - cleans the build directory and all bootstrap related directories.
- `buildRpm` - builds an RPM package for RHEL based Linux distros.
- `buildDeb` - builds a DEB package for Debian based Linux distros.
- `packages` - executes both `buildRpm` and `buildDeb` tasks.
- `getjenkins` - Downloads `jenkins.war` dependency and copies it to the current
  directory.
- `getplugins` - Downloads Jenkins plugin HPI files and copies it to
  `$PWD/plugins`.


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
