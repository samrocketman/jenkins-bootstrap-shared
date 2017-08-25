/*
   Copyright (c) 2015-2017 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
     - Number of master executors
     - Master labels
     - Master usage (e.g. exclusive or any job runs on master)
     - TCP port for JNLP slave agents
 */

import jenkins.model.Jenkins
import jenkins.model.JenkinsLocationConfiguration
import net.sf.json.JSONObject

if(!binding.hasVariable('master_settings')) {
    master_settings = [:]
}
if(!(master_settings instanceof Map)) {
    throw new Exception('master_settings must be a Map.')
}

master_settings = master_settings as JSONObject

def requiredDefaultValues(def value, List values, def default_value) {
    (value in values)? value : default_value
}

//settings with sane defaults
String frontend_url = master_settings.optString('frontend_url', 'http://localhost:8080/')
String admin_email = master_settings.optString('admin_email')
String system_message = master_settings.optString('system_message')
int quiet_period = master_settings.optInt('quiet_period', 5)
int scm_checkout_retry_count = master_settings.optInt('scm_checkout_retry_count', 0)
int master_executors = master_settings.optInt('master_executors', 2)
String master_labels = master_settings.optString('master_labels')
String master_usage = requiredDefaultValues(master_settings.optString('master_usage').toUpperCase(), ['NORMAL', 'EXCLUSIVE'], 'NORMAL')
int jnlp_slave_port = master_settings.optInt('jnlp_slave_port', -1)

Jenkins j = Jenkins.instance
JenkinsLocationConfiguration location = j.getExtensionList('jenkins.model.JenkinsLocationConfiguration')[0]
Boolean save = false

if(location.url != frontend_url) {
    println "Updating Jenkins URL to: ${frontend_url}"
    location.url = frontend_url
    save = true
}
if(location.adminAddress != admin_email) {
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
if(j.numExecutors != master_executors) {
    println "Setting master num executors to: ${master_executors}"
    j.numExecutors = master_executors
    save = true
}
if(j.labelString != master_labels) {
    println "Setting master labels to: ${master_labels}"
    j.setLabelString(master_labels)
    save = true
}
if(j.mode.toString() != master_usage) {
    println "Setting master usage to: ${master_usage}"
    j.mode = Node.Mode."${master_usage}"
    save = true
}
if(j.slaveAgentPort != jnlp_slave_port) {
    if(jnlp_slave_port <= 65535 && jnlp_slave_port >= -1) {
        println "Set JNLP Slave port: ${jnlp_slave_port}"
        j.slaveAgentPort = jnlp_slave_port
        save = true
    }
    else {
        println "WARNING: JNLP port ${jnlp_slave_port} outside of TCP port range.  Must be within -1 <-> 65535.  Nothing changed."
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
