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
  cp -r Vagrantfile .gitignore gradle* build.gradle variables.gradle env.sh ../
  mkdir -p ../scripts
  cp scripts/vagrant-up.sh ../scripts/
  $SED 's#^\( \+\)\.\(/scripts/upgrade/.*\)$#\1jenkins-bootstrap-shared\2#' README.md > ../README.md
)

echo 'bootstrapHome=jenkins-bootstrap-shared' > gradle.properties
cat > jenkins_bootstrap.sh <<'EOF'
#!/bin/bash
source jenkins-bootstrap-shared/jenkins_bootstrap.sh
EOF
chmod 755 jenkins_bootstrap.sh
