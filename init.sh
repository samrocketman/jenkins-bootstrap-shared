#!/bin/bash

if [ ! -d jenkins-bootstrap-shared -o ! -d .git ] || ! grep jenkins-bootstrap-shared .gitmodules &> /dev/null; then
  echo "ERROR: no submodule jenkins-bootstrap-shared" >&2
  exit 1
fi

if [ -f build.gradle ]; then
  echo "ERROR: build.gradle exists; already initialized bootstrap." >&2
  exit 1
fi

#choose GNU sed on Mac (installed via homebrew) or fall back to sed on GNU/Linux
if [ -x "$(type -P gsed)" ]; then
  SED=gsed
else
  SED=sed
fi

(
  cd jenkins-bootstrap-shared/
  cp -r .gitignore gradle* build.gradle variables.gradle ../
)

echo 'bootstrapHome=jenkins-bootstrap-shared' > gradle.properties

#${SED} -i.bak -r -- $'s#^(apply from: \')(shared.gradle\')$#\\1jenkins-bootstrap-shared/\\2\\napply from: \'variables.gradle\'#' build.gradle
#rm -f build.gradle.bak
