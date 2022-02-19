/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Will upgrade only Jenkins core assuming it has no plugins installed.  This
   script supports the plugin refresh method.  See plugin_refresh.md
   documentation.
 */
import hudson.model.UpdateCenter
import jenkins.model.Jenkins

//Check for the latest update center updates from jenkins.io
Jenkins.instance.pluginManager.doCheckUpdatesServer()

//get the current update center
UpdateCenter center = Jenkins.instance.updateCenter

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
null
