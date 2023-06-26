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
    Sets the primary view of Jenkins.

    Example Usage:
        curl --data-urlencode "script=$(<./scripts/configure-primary-view.groovy)" http://localhost:8080/scriptText
 */
import hudson.model.View
import jenkins.model.Jenkins

List<PluginWrapper> plugins = Jenkins.instance.pluginManager.getPlugins()
//get a list of installed plugins
Set<String> installed_plugins = []
plugins.each {
    installed_plugins << it.getShortName()
}

Set<String> required_plugins = ['dashboard-view', 'view-job-filters']

Boolean hasConfigBeenUpdated = false

//only execute of all required plugins are installed
if((required_plugins-installed_plugins).size() == 0) {
    Jenkins instance = Jenkins.getInstance()
    View welcome_view = instance.getView('Welcome')
    View all_view = instance.getView('All')
    View primary_view = instance.getPrimaryView()
    if(welcome_view != null) {
        if(!primary_view.name.equals(welcome_view.name)) {
            println 'Set primary view to "Welcome".'
            instance.setPrimaryView(welcome_view)
            hasConfigBeenUpdated = true
        } else {
            println 'Nothing changed.  Primary view already set to "Welcome".'
        }
        if(all_view != null) {
            println 'Deleting "All" view.'
            instance.deleteView(all_view)
            hasConfigBeenUpdated = true
        }
        //save configuration to disk
        if(hasConfigBeenUpdated) {
            instance.save()
        }
    } else {
        println '"Welcome" view not found.  Nothing changed.'
    }
} else {
    println 'Unable to configure primary view.'
    println "Missing required plugins: ${required_plugins-installed_plugins}"
}
