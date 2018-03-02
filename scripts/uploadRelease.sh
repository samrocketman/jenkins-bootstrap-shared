#!/bin/bash
#Created by Sam Gleske
#Thu Feb 15 21:10:20 PST 2018
#Linux 4.13.0-32-generic x86_64
#GNU bash, version 4.3.48(1)-release (x86_64-pc-linux-gnu)

#Upload GitHub releases

if [ -z "${GITHUB_TOKEN}" -o -z "${GITHUB_USER}" -o -z "${GITHUB_REPO}" ]; then
  echo $'ERROR: Missing required environment variables:\n  - GITHUB_TOKEN\n  - GITHUB_USER\n  - GITHUB_REPO'
  [ -z "${!GITHUB_*}" ] || echo "You have defined: ${!GITHUB_*}"
  exit 1
fi

export GITHUB_API="${GITHUB_API:-https://api.github.com}"
GITHUB_API="${GITHUB_API%/}"

function tempdir() {
  TMP_DIR=$(mktemp -d)
  PATH="${TMP_DIR}:${PATH}"
  export TMP_DIR PATH
}

function checkGHRbin() {
  local url sha256
  case $(uname -s) in
    Linux)
      url='https://github.com/aktau/github-release/releases/download/v0.7.2/linux-amd64-github-release.tar.bz2'
      sha256='3feae868828f57a24d1bb21a5f475de4a116df3063747e3290ad561b4292185c'
      ;;
    Darwin)
      url='https://github.com/aktau/github-release/releases/download/v0.7.2/darwin-amd64-github-release.tar.bz2'
      sha256='92d7472d6c872aa5f614c5141e84ee0a67fbdae87c0193dcf0a0476d9f1bc250'
      ;;
  esac
  if ! type -P github-release && [ -n "${sha256}" ]; then
    pushd "${TMP_DIR}"
    curl -LO "${url}"
    sha256sum -c - <<< "${sha256}  ${url##*/}"
    tar --transform 's/.*\///g' -xjf "${url##*/}"
    \rm "${url##*/}"
    popd
  fi
}

#exit non-zero if no "repo" or "public_repo" OAuth scope found for API token
function checkOAuthScopes() {
  set  +ex
  curl -sIH "Authorization: token $GITHUB_TOKEN" "${GITHUB_API}/" |
  awk '
  BEGIN {
    code=1
  };
  $0 ~ /^X-OAuth-Scopes:.*\y(public_)?repo,?\y.*/ {
    code=0;
    exit
  };
  END { exit code }
  '
  local code=$?
  set -ex
  return ${code}
}

function read_err_on() {
  set +x
  exec >&2
  case $1 in
    5)
      echo "ERROR: must specify a tag.  example: ${0##*/} v1.0"
      ;;
    10)
      echo 'ERROR: must be in root of repository to build a release.'
      ;;
    11)
      echo 'ERROR: sha256sum command is missing.'
      ;;
    12)
      echo 'ERROR: mktemp command is missing.'
      ;;
    13)
      echo "ERROR: Local git tag ${2} does not exist."
      ;;
    14)
      echo "ERROR: Remote git tag ${2} does not exist."
      ;;
    15)
      echo $'ERROR: GITHUB_TOKEN must have one of the following scopes:\n  - repo\n  - public_repo'
      ;;
    16)
      echo 'ERROR: github-release does not exist and could not be downloaded.'
      ;;
    0)
      echo 'SUCCESS: all files released.'
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
tempdir
set -ex

#pre-flight tests (any failures will exit non-zero)
[ -n "${1}" ] || exit 5
type -P sha256sum || exit 11
type -P mktemp || exit 12
git tag | grep "${1}" || exit 13
git ls-remote | grep "refs/tags/${1}" || exit 14
checkOAuthScopes || exit 15
checkGHRbin
type -P github-release || exit 16

cd build/distributions/

#cut a release
github-release release --tag "${1}"
#upload all files in parallel
ls -1 | xargs -n1 -P0 -I{} -- \
github-release upload --tag "${1}" --name '{}' --file '{}'
