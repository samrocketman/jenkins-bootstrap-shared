# Distribution Packages

Parts or all scripts were taken from https://github.com/jenkinsci/packaging

This provides OS-dependent packages which include the exact versions of Jenkins
and exact versions of plugins.  This provides more repeatable means of upgrading
Jenkins within an immutable infrastructure environment.

### Packaging RPM

To create an RPM package.

    ./gradlew clean buildRpm

To inspect the package.

    rpm -qip --dump --scripts build/distributions/*.rpm | less

List package dependencies.

    rpm -qRp build/distributions/*.rpm

### Packaging DEB

To create a DEB package.

    ./gradlew clean buildDeb

To inpsect the package.

    dpkg -I build/distributions/*.deb

Dry run to see what dpkg would do.

    dpkg --dry-run -i build/distributions/*.deb

Extract only the control information files from the package.

    dpkg -e build/distributions/*.deb

### Build all

Build all available package formats.

    ./gradlew clean packages
