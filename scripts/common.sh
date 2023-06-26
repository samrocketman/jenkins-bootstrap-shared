#!/bin/bash
#Created by Sam Gleske (https://github.com/samrocketman)
#Copyright (c) 2015-2023 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis
#Sun Jul 26 14:30:25 EDT 2015
#Ubuntu 14.04.2 LTS
#Linux 3.13.0-57-generic x86_64
#GNU bash, version 4.3.11(1)-release (x86_64-pc-linux-gnu)
#curl 7.35.0 (x86_64-pc-linux-gnu) libcurl/7.35.0 OpenSSL/1.0.1f zlib/1.2.8 libidn/1.28 librtmp/2.3

export CURL="${CURL:-curl}"
export JENKINS_WEB="${JENKINS_WEB:-http://localhost:8080}"
export SCRIPT_LIBRARY_PATH="${SCRIPT_LIBRARY_PATH:-./scripts}"

function jenkins_console() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  script=()
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          jenkins="$1"
          shift
        ;;
      -s|--script)
          shift
          script+=(-d "$1")
          shift
        ;;
    esac
  done
  if [ "${#script}" -eq 0 ]; then
    echo 'ERROR Missing --script SCRIPT for jenkins_console() function.'
    exit 1
  fi

  "${SCRIPT_LIBRARY_PATH}"/jenkins_call.sh -m POST --data-string script= "${script[@]}" "${jenkins}"
)

function curl_item_script() (
  set -euo pipefail
  #parse options
  jenkins="${JENKINS_WEB}/scriptText"
  args=()
  while [ ! -z "${1:-}" ]; do
    case $1 in
      -j|--jenkins)
          shift
          args+=(--jenkins "$1")
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
  jenkins_console -s <(echo "String itemName='${item_name}';String xmlData='''$(< "${xml_data}")''';") -s "${script}" "${args[@]:-}"
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
  curl_item_script -n "${job_name}" -x "${xml_data}" -s "${SCRIPT_LIBRARY_PATH}/create-job.groovy"
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
  curl_item_script -n "${view_name}" -x "${xml_data}" -s "${SCRIPT_LIBRARY_PATH}/create-view.groovy"
)
