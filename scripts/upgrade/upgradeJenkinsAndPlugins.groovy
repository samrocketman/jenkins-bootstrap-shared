/*
   Copyright (c) 2015-2023 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-slack

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
   Initiate an asynchronous upgrade of Jenkins core and plugins.  This script
   will only upgrade if an upgrade is necessary.  This script will not attempt
   to upgrade plugins which have already been upgraded.
 */
import hudson.model.UpdateCenter

//Check for the latest update center updates from jenkins.io
Jenkins.instance.pluginManager.doCheckUpdatesServer()

//get the current update center
UpdateCenter center = Jenkins.instance.updateCenter
while(!center.isSiteDataReady()) {
    sleep(500)
}

//upgrade Jenkins core
//fake emulate a stapler request
def req = [
	sendRedirect2: { String redirect -> null }
] as org.kohsuke.stapler.StaplerResponse
//detect if Jenkins core needs an upgrade
if(center.getCoreSource().data && center.getCoreSource().data.hasCoreUpdates() && !center.getHudsonJob()) {
    println "Upgrading Jenkins Core."
    center.doUpgrade(req)
}

//schedule an upgrade of all plugins
print "Upgrading Plugins: "
println center.updates.findAll { center.getJob(it) == null }.each { it.deploy(false) }*.name
null
