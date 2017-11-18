#!/bin/bash
#This script is meant to be run from within a centos:7 docker container

set -euo pipefail

PACKAGES=(java-1.8.0-openjdk-devel git)
GOSS_SHASUM=5669df08e406abf594de0e7a7718ef389e5dc7cc76905e7f6f64711e6aad7fa3
GOSS_URL=https://github.com/aelsabbahy/goss/releases/download/v0.3.5/goss-linux-amd64

#install packages
rpm -q "${PACKAGES[@]}" || yum install -y "${PACKAGES[@]}"
export JAVA_HOME=/usr/lib/jvm/java-1.8.0

# build rpm
[ -f build/distributions/*.rpm ] || ./gradlew buildRpm
! rpm -q jenkins-bootstrap
yum localinstall -y build/distributions/*.rpm

#install goss
[ -x '/sbin/goss' ] || (
  curl -Lo /sbin/goss "${GOSS_URL}" &&
  sha256sum -c - <<<"${GOSS_SHASUM}  /sbin/goss" &&
  chmod 755 /sbin/goss
)

cd tests/rpm
goss validate
