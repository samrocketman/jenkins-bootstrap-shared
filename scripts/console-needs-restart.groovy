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
   Checks if Jenkins needs a restart after plugins were installed.

   Example Usage:
        curl -s --data-urlencode "script=$(<scripts/console-needs-restart.groovy)" http://localhost:8080/scriptText
 */

//what plugins should be installed (by plugin ID)
Set<String> plugins_to_install = [
    "git", "github", "github-oauth", "token-macro",
    "cloudbees-folder", "job-dsl", "view-job-filters",
    "embeddable-build-status", "groovy", "dashboard-view", "rich-text-publisher-plugin", "console-column-plugin", "docker-plugin"
]

List<PluginWrapper> plugins = Jenkins.instance.pluginManager.getPlugins()

//get a list of installed plugins
Set<String> installed_plugins = []
plugins.each {
    installed_plugins << it.getShortName()
}

//get a list of plugins needing an update
Set<String> plugins_to_update = []
plugins.each {
    if(it.hasUpdate()) {
        plugins_to_update << it.getShortName()
    }
}

//print out a statement for bash
if(plugins_to_update.size() > 0 || (plugins_to_install-installed_plugins).size() > 0) {
    println "true"
} else {
    println "false"
}
