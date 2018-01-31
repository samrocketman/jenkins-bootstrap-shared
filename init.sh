#!/bin/bash

if [ ! -d jenkins-bootstrap-shared -o ! -d .git ] || ! grep jenkins-bootstrap-shared .gitmodules &> /dev/null; then
  echo "ERROR: no submodule jenkins-bootstrap-shared" >&2
  exit 1
fi

if [ -f build.gradle ]; then
  echo "ERROR: build.gradle exists; already initialized bootstrap." >&2
  exit 1
fi

#GNU sed is required.  Homebrew on Mac installs GNU sed as gsed.
#Try to detect gsed; otherwise, fall back to just using sed.
[ -x "$(type -P gsed)" ] && SED=gsed || SED=sed
export SED

(
  cd jenkins-bootstrap-shared/
  cp -r Vagrantfile .gitignore gradle* build.gradle variables.gradle dependencies.gradle env.sh ../
  mkdir -p ../scripts
  cp scripts/vagrant-up.sh ../scripts/
  $SED 's#^\( \+\.\)\(/scripts/upgrade/.*\)$#\1/jenkins-bootstrap-shared\2#' README.md > ../README.md
  $SED 's#\([^:]*:\) \(Dockerfile\)#\1 jenkins-bootstrap-shared/\2#' docker-compose.yml > ../docker-compose.yml
)

echo 'bootstrapHome=jenkins-bootstrap-shared' > gradle.properties
cat > jenkins_bootstrap.sh <<'EOF'
#!/bin/bash
source jenkins-bootstrap-shared/jenkins_bootstrap.sh
EOF
chmod 755 jenkins_bootstrap.sh
