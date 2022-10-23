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
   This script will configure global settings for a Jenkins instance.  These
   are all settings built into core.

   Configures Global settings:
     - Jenkins URL
     - Admin email
     - System message
     - Quiet Period
     - SCM checkout retry count
     - Number of controller executors
     - Controller labels
     - Controller usage (e.g. exclusive requiring built-in label or any job could run by default)
     - TCP port for JNLP build agents
 */

import jenkins.model.Jenkins
import jenkins.model.JenkinsLocationConfiguration
import net.sf.json.JSONObject

if(!binding.hasVariable('controller_settings')) {
    controller_settings = [:]
}
if(!(controller_settings instanceof Map)) {
    throw new Exception('controller_settings must be a Map.')
}

controller_settings = controller_settings as JSONObject

def requiredDefaultValues(def value, List values, def default_value) {
    (value in values)? value : default_value
}

//settings with sane defaults
String frontend_url = controller_settings.optString('frontend_url', 'http://localhost:8080/')
String admin_email = controller_settings.optString('admin_email')
String system_message = controller_settings.optString('system_message')
int quiet_period = controller_settings.optInt('quiet_period', 5)
int scm_checkout_retry_count = controller_settings.optInt('scm_checkout_retry_count', 0)
int controller_executors = controller_settings.optInt('controller_executors', 2)
String controller_labels = controller_settings.optString('controller_labels')
String controller_usage = requiredDefaultValues(controller_settings.optString('controller_usage').toUpperCase(), ['NORMAL', 'EXCLUSIVE'], 'EXCLUSIVE')
int jnlp_agent_port = controller_settings.optInt('jnlp_agent_port', -1)

Jenkins j = Jenkins.instance
JenkinsLocationConfiguration location = j.getExtensionList('jenkins.model.JenkinsLocationConfiguration')[0]
Boolean save = false

if(location.url != frontend_url) {
    println "Updating Jenkins URL to: ${frontend_url}"
    location.url = frontend_url
    save = true
}
if(admin_email && location.adminAddress != admin_email) {
    println "Updating Jenkins Email to: ${admin_email}"
    location.adminAddress = admin_email
    save = true
}
if(j.systemMessage != system_message) {
    println 'System message has changed.  Updating message.'
    j.systemMessage = system_message
    save = true
}
if(j.quietPeriod != quiet_period) {
    println "Setting Jenkins Quiet Period to: ${quiet_period}"
    j.quietPeriod = quiet_period
    save = true
}
if(j.scmCheckoutRetryCount != scm_checkout_retry_count) {
    println "Setting Jenkins SCM checkout retry count to: ${scm_checkout_retry_count}"
    j.scmCheckoutRetryCount = scm_checkout_retry_count
    save = true
}
if(j.numExecutors != controller_executors) {
    println "Setting controller num executors to: ${controller_executors}"
    j.numExecutors = controller_executors
    save = true
}
if(j.labelString != controller_labels) {
    println "Setting controller labels to: ${controller_labels}"
    j.setLabelString(controller_labels)
    save = true
}
if(j.mode.toString() != controller_usage) {
    println "Setting controller usage to: ${controller_usage}"
    j.mode = Node.Mode."${controller_usage}"
    save = true
}
if(j.slaveAgentPort != jnlp_agent_port) {
    if(jnlp_agent_port <= 65535 && jnlp_agent_port >= -1) {
        println "Set JNLP Agent port: ${jnlp_agent_port}"
        j.slaveAgentPort = jnlp_agent_port
        save = true
    }
    else {
        println "WARNING: JNLP port ${jnlp_agent_port} outside of TCP port range.  Must be within -1 <-> 65535.  Nothing changed."
    }
}
//save configuration to disk
if(save) {
    j.save()
    location.save()
}
else {
    println 'Nothing changed.  Jenkins settings already configured.'
}
