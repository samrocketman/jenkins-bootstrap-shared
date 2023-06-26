/*
   Copyright (c) 2015-2023 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */

/*
   Prints out plugins that need to be downloaded from the current instance.
 */

import groovy.json.JsonSlurper
def json = new JsonSlurper()
def addr="http://jenkins-updates.cloudbees.com/update-center.json"
def updateCenterPlugins=json.parseText((new URL(addr).newReader().readLines())[1..-1].join(''))["plugins"]
Jenkins.instance.pluginManager.plugins.each {
    def plugin=it.getShortName()
    println "getplugins 'org.jenkins-ci.plugins:${updateCenterPlugins[plugin]["name"]}:${updateCenterPlugins[plugin]["version"]}'"
}
