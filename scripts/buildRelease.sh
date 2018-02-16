#!/bin/bash
#Created by Sam Gleske
#Thu Feb 15 21:10:20 PST 2018
#Linux 4.13.0-32-generic x86_64
#GNU bash, version 4.3.48(1)-release (x86_64-pc-linux-gnu)

#Build release binaries

function read_err_on() {
  set +x
  exec >&2
  case $1 in
    10)
      echo 'ERROR: must be in root of repository to build a release.'
      ;;
    11)
      echo 'ERROR: sha256sum command is missing.'
      ;;
    20)
      echo 'ERROR: Build "succeeded" but did not produce proper output.'
      ;;
    0)
      echo 'SUCCESS: all release files located in build/distributions/.'
      ;;
    *)
      echo 'ERROR: an error occured.'
      ;;
  esac
  if [ -n "${TMP_DIR}" -a -d "${TMP_DIR}" ]; then
    rm -rf "${TMP_DIR}"
  fi
}
trap 'read_err_on $? "${1}"' EXIT
set -ex

#pre-flight tests (any failures will exit non-zero)
[ -f build.gradle ] || exit 10
type -P sha256sum || exit 11

./gradlew clean packages
[ -d build/distributions ] || exit 20
cd build/distributions/
sha256sum -- * > sha256sum.txt
cp ../jenkins-versions.manifest ./
