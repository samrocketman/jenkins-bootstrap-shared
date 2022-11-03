#!/bin/bash
# This is a wrapper script for jenkins-call-url and jenkins-call-url-2.7.  This
# script will detect the python version from the current system.  It selects an
# appropriate way to call jenkins-call-url.  This enables a better user
# experience by not having to worry about python version.

function minimum() (
  exec &> /dev/null
  local major="${2%.*}"
  local minor="${2#*.}"
  if ! type -P "$1"; then
    return false
  fi
  python -c 'import platform,sys; check=lambda x,y,z: x.startswith(y) and int(x.split(".")[0:2][-1]) >= z; sys.exit(0) if check(platform.python_version(), sys.argv[1], int(sys.argv[2])) else sys.exit(1)' \
  "${major}" "${minor}"
)

if minimum python3 '3.8'; then
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "$@"
elif minimum python '3.8'; then
  python "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url "$@"
elif minimum python '2.7'; then
  "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url-2.7 "$@"
else
  echo 'Python 2 or 3.8+ could not be detected.' >&2
  exit 1
fi
