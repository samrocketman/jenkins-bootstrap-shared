#!/usr/bin/env python
#Created by Sam Gleske
#Meant to read from stdin a JSON blob from Jenkins master root
import json
import sys
response = json.load(sys.stdin)
jobs = map(lambda x: x['name'], response['jobs'])
views = map(lambda x: x['name'], response['views'])
assert '_jervis_generator' in jobs
assert 'GitHub Organizations' in views
assert 'Welcome' in views
assert 'Welcome' == response['primaryView']['name']
