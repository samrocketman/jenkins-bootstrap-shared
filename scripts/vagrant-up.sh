#!/bin/bash
# Created by Sam Gleske
# This script is meant to run before "vagrant up".  It will check if there's an
# RPM already built and, if not, build it.

# Please keep in mind that this script runs on _every_ vagrant command.
# Therefore, idempotency is very important.  Only output to stderr or stdout
# when there's an error or when real commands are being executed.  No output
# means the script successfully and silently skipped everything.

if [ ! -f 'build.gradle' -a ! -d '.git' ]; then
  echo "Error: not run from root of repository." >&2
  exit 1
fi

# If the RPM doesn't exist then build it.
if [ ! -f build/distributions/*.rpm ]; then
  ./gradlew buildRpm
fi
