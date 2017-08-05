# jenkins-bootstrap shared scripts

These scripts allow an easily source-able upstream project which allow Jenkins
to be bootstrapped.

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
