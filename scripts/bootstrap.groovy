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
import hudson.model.UpdateSite
import hudson.PluginWrapper

/*
   What is the purpose of this script?  It does the following when run in the
   Jenkins script console.
     * Checks for settings and if they're different then updates them.
     * Checks for outdate plugins and upgrades them.
     * Checks for desired plugins and if missing installs them.
     * If any configuration has changed then save the configuration to disk.

    Examle Usage:
        curl --data-urlencode "script=$(<./scripts/bootstrap.groovy)" http://localhost:8080/scriptText
 */

/*
   Interesting settings
 */

//what plugins should be installed (by plugin ID)
Set<String> plugins_to_install = [
    "git", "github", "github-oauth", "token-macro",
    "cloudbees-folder", "job-dsl", "view-job-filters",
    "embeddable-build-status", "groovy", "dashboard-view", "rich-text-publisher-plugin", "console-column-plugin", "docker-plugin"
]
//should we dynamically load plugins when installed?
Boolean dynamicLoad = false

/*
   Install Jenkins plugins
 */
def install(Collection c, Boolean dynamicLoad, UpdateSite updateSite) {
    c.each {
        println "Installing ${it} plugin."
        UpdateSite.Plugin plugin = updateSite.getPlugin(it)
        Throwable error = plugin.deploy(dynamicLoad).get().getError()
        if(error != null) {
            println "ERROR installing ${it}, ${error}"
        }
    }
    null
}

/*
   Make it so...
 */

//some useful vars to set
Boolean hasConfigBeenUpdated = false

//check to see if using Jenkins Enterprise and if so then use its update site
Set<String> update_sites = []
Jenkins.getInstance().getUpdateCenter().getSiteList().each {
    update_sites << it.id
}
UpdateSite updateSite
if('jenkins-enterprise' in update_sites) {
    updateSite = Jenkins.getInstance().getUpdateCenter().getById('jenkins-enterprise')
} else {
    updateSite = Jenkins.getInstance().getUpdateCenter().getById('default')
}
println "Using update site ${updateSite.id}."

List<PluginWrapper> plugins = Jenkins.instance.pluginManager.getPlugins()

//check the update site(s) for latest plugins
println 'Checking plugin updates via Plugin Manager.'
Jenkins.instance.pluginManager.doCheckUpdatesServer()

//disable submitting usage statistics for privacy
if(Jenkins.instance.isUsageStatisticsCollected()) {
    println "Disable submitting anonymous usage statistics to jenkins-ci.org for privacy."
    Jenkins.instance.setNoUsageStatistics(true)
    hasConfigBeenUpdated = true
}

//any plugins need updating?
Set<String> plugins_to_update = []
plugins.each {
    if(it.hasUpdate()) {
        plugins_to_update << it.getShortName()
    }
}
if(plugins_to_update.size() > 0) {
    println "Updating plugins..."
    install(plugins_to_update, dynamicLoad, updateSite)
    println "Done updating plugins."
    hasConfigBeenUpdated = true
}


//get a list of installed plugins
Set<String> installed_plugins = []
plugins.each {
    installed_plugins << it.getShortName()
}

//check to see if there are missing plugins to install
Set<String> missing_plugins = plugins_to_install - installed_plugins
if(missing_plugins.size() > 0) {
    println "Install missing plugins..."
    install(missing_plugins, dynamicLoad, updateSite)
    println "Done installing missing plugins."
    hasConfigBeenUpdated = true
}

if(hasConfigBeenUpdated) {
    println "Saving Jenkins configuration to disk."
    Jenkins.instance.save()
} else {
    println "Jenkins up-to-date.  Nothing to do."
}
