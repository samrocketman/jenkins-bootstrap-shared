#Created by Sam Gleske (https://github.com/samrocketman)
#Copyright (c) 2015-2017 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis
#Sun Jul 26 14:30:25 EDT 2015
#Ubuntu 14.04.2 LTS
#Linux 3.13.0-57-generic x86_64
#GNU bash, version 4.3.11(1)-release (x86_64-pc-linux-gnu)
#curl 7.35.0 (x86_64-pc-linux-gnu) libcurl/7.35.0 OpenSSL/1.0.1f zlib/1.2.8 libidn/1.28 librtmp/2.3

export CURL="${CURL:-curl}"
export JENKINS_WEB="${JENKINS_WEB:-http://localhost:8080}"
export SCRIPT_LIBRARY_PATH="${SCRIPT_LIBRARY_PATH:-./scripts}"

function curl_item_script() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          jenkins="$1"
          shift
        ;;
      -s|--script)
          shift
          script="$1"
          shift
        ;;
      -x|--xml-data)
          shift
          xml_data="$1"
          shift
        ;;
      -n|--item-name)
          shift
          item_name="$1"
          shift
        ;;
    esac
  done
  if [ -z "${script:-}" -o -z "${xml_data:-}" -o -z "${item_name}" ]; then
    echo 'ERROR Missing an option for curl_item_script() function.'
    exit 1
  fi
  ${CURL} --data-urlencode "script=String itemName='${item_name}';String xmlData='''$(<${xml_data})''';$(<${script})" ${jenkins}
)

function jenkins_console() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          jenkins="$1"
          shift
        ;;
      -s|--script)
          shift
          script="$1"
          shift
        ;;
    esac
  done
  if [ -z "${script:-}" ]; then
    echo 'ERROR Missing --script SCRIPT for jenkins_console() function.'
    exit 1
  fi
  ${SCRIPT_LIBRARY_PATH}/jenkins-call-url -m POST --data-string script= -d "${script}" ${jenkins}
)

function create_job() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          jenkins="$1"
          shift
        ;;
      -x|--xml-data)
          shift
          xml_data="$1"
          shift
        ;;
      -n|--job-name)
          shift
          job_name="$1"
          shift
        ;;
    esac
  done
  if [ -z "${xml_data:-}" -o -z "${job_name}" ]; then
    echo 'ERROR Missing an option for create_job() function.'
    exit 1
  fi
  curl_item_script --item-name "${job_name}" \
    --xml-data "${xml_data}" \
    --script "${SCRIPT_LIBRARY_PATH}/create-job.groovy"
)

function create_view() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          jenkins="$1"
          shift
        ;;
      -x|--xml-data)
          shift
          xml_data="$1"
          shift
        ;;
      -n|--view-name)
          shift
          view_name="$1"
          shift
        ;;
    esac
  done
  if [ -z "${xml_data:-}" -o -z "${view_name}" ]; then
    echo 'ERROR Missing an option for create_job() function.'
    exit 1
  fi
  curl_item_script --item-name "${view_name}" \
    --xml-data "${xml_data}" \
    --script "${SCRIPT_LIBRARY_PATH}/create-view.groovy"
)

#CSRF protection support
function is_crumbs_enabled() {
  use_crumbs="$( $CURL -s ${JENKINS_WEB}/api/json?pretty=true 2> /dev/null | python -c 'import sys,json;exec "try:\n  j=json.load(sys.stdin)\n  print str(j[\"useCrumbs\"]).lower()\nexcept:\n  pass"' )"
  if [ "${use_crumbs}" = "true" ]; then
    return 0
  fi
  return 1
}

#CSRF protection support
function get_crumb() {
  ${CURL} -s ${JENKINS_WEB}/crumbIssuer/api/json | python -c 'import sys,json;j=json.load(sys.stdin);print j["crumbRequestField"] + "=" + j["crumb"]'
}

#CSRF protection support
function csrf_set_curl() {
  if is_crumbs_enabled; then
    if [ ! "${CSRF_CRUMB}" = "$(get_crumb)" ]; then
      if [ -n "${CSRF_CRUMB}" ]; then
        #remove existing crumb value from curl command
        CURL="$(echo "${CURL}" | sed "s/ -d ${CSRF_CRUMB}//")"
      fi
      export CSRF_CRUMB="$(get_crumb)"
      export CURL="${CURL} -d ${CSRF_CRUMB}"
      echo "Using crumbs for CSRF support."
    elif ! echo "${CURL}" | grep -F "${CSRF_CRUMB}" &> /dev/null; then
      export CURL="${CURL} -d ${CSRF_CRUMB}"
      echo "Using crumbs for CSRF support."
    fi
  fi
}

function is_auth_enabled() {
  no_authentication="$( $CURL -s ${JENKINS_WEB}/api/json?pretty=true 2> /dev/null | python -c 'import sys,json;exec "try:\n  j=json.load(sys.stdin)\n  print str(j[\"useSecurity\"]).lower()\nexcept:\n  pass"' )"
  #check if authentication is required.;
  #if the value of no_authentication is anything but false; then assume authentication
  if [ ! "${no_authentication}" = "false" ]; then
    echo -n "Authentication required..."
    if [ -e "${JENKINS_HOME}/secrets/initialAdminPassword" ]; then
      echo "DONE"
      return 0
    else
      echo "FAILED"
      echo "Could not set authentication."
      echo "Missing file: ${JENKINS_HOME}/secrets/initialAdminPassword"
      exit 1
    fi
  fi
  return 1
}
