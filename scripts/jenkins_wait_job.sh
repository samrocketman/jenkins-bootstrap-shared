#!/bin/bash
#Created by Sam Gleske (https://github.com/samrocketman/home)
#Ubuntu 16.04.2 LTS
#Linux 4.4.0-72-generic x86_64
#Python 2.7.12

function json() {
  python -c "import sys,json;print str(json.load(sys.stdin)[\"${1}\"]).lower()"
}

MESSAGE='Jobb success.'
PIPELINE_INPUT=false
count=0
while true; do
  [ "$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url ${1%/}/api/json | json building)" = 'false' ] && break
  if [ "$count" -eq "0" ]; then
    if ( "${SCRIPT_LIBRARY_PATH}"/jenkins-call-url ${1%/}/consoleText | tail | grep 'Input requested' ); then
      PIPELINE_INPUT=true
      break
    fi
  fi
  echo "building..."
  #every 15 seconds check consoleText
  ((count++, count = count%3)) || true
  sleep 5
done

if ${PIPELINE_INPUT}; then
  RESULT=SUCCESS
  MESSAGE='Pipeline input requested.'
else
  RESULT=$("${SCRIPT_LIBRARY_PATH}"/jenkins-call-url ${1%/}/api/json | json result | tr 'a-z' 'A-Z')
fi

"${SCRIPT_LIBRARY_PATH}"/jenkins-call-url ${1%/}/consoleText

#script exit code is last command
[ "${RESULT}" = 'SUCCESS' ]
