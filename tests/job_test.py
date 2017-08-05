#!/usr/bin/env python
#Created by Sam Gleske
#Reads Jenkins lastBuild API input until a build is finished.
#Prints out console log when build is finished.
#Exits non-zero if failed build or zero if success.

import base64
import json
import os.path
import sys
import time
import urllib2

headers={
    'Host': 'localhost'
}

#Check for Jenkins 2.0 authentication
jenkins_home=os.environ['JENKINS_HOME']
if os.path.isfile(jenkins_home + '/secrets/initialAdminPassword'):
    with open(jenkins_home + '/secrets/initialAdminPassword', 'r') as f:
        password = f.read().strip()
    headers['Authorization'] = "Basic %s" % base64.b64encode("admin:%s" % password)

#CSRF protection support
csrf_crumb=os.environ['CSRF_CRUMB']
if len(csrf_crumb) > 0:
    headers[csrf_crumb.split('=')[0]] = csrf_crumb.split('=')[1]

#assume always first argument and first argument is a proper URL to Jenkins
response = {}
STATUS = 0
while True:
    req = urllib2.Request(url=sys.argv[1], headers=headers)
    url = urllib2.urlopen(req)
    response = json.load(url)
    url.close()
    if not response['building']:
        break
    time.sleep(1)

console_url = response['url'] + 'consoleText'
if len(os.environ['JENKINS_WEB']) > 0:
    jenkins_host = os.environ['JENKINS_WEB'].split('/')[2]
    console_url = console_url.split('/')
    console_url[2] = jenkins_host
    console_url = '/'.join(console_url)
print "Retrieving %s" % console_url
req = urllib2.Request(url=console_url, headers=headers)
url = urllib2.urlopen(req)
console = url.read()
url.close()

if 'WARNING' in console:
    STATUS = 1

if 'SUCCESS' != response['result']:
    STATUS = 1

print console
sys.exit(STATUS)
