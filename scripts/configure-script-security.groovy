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
   Configure script-security plugin settings.

   Requirements:
   * Add (newly) approved script signatures to the list in configs/script-security-settings.groovy.
   * Compatible with script-security v1.73

   Reference APIs:

   * https://github.com/jenkinsci/script-security-plugin/blob/master/src/main/java/org/jenkinsci/plugins/scriptsecurity/scripts/ScriptApproval.java

 */

import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

// CONFIGURATION AS CODE
if(!binding.hasVariable('script_security_settings')) {
    println 'Configure script-security plugin by modifying configs/script-security-settings.groovy.'
    script_security_settings = [:]
}

if(!(script_security_settings instanceof Map)) {
    throw new Exception('script_security_settings must be a Map.')
}
ScriptApproval scriptApprovalConfig = Jenkins.instance.getExtensionList(ScriptApproval)[0]

List newlyApprovedSignatures = ((script_security_settings['approvedSignatures'])?:[])*.trim().sort()
List existingApprovedSignatures = scriptApprovalConfig.getApprovedSignatures().toList()*.trim().sort()

// build a full list of signatures
newlyApprovedSignatures = (newlyApprovedSignatures - existingApprovedSignatures).sort().unique()

List signaturesToApprove = newlyApprovedSignatures + existingApprovedSignatures

if (signaturesToApprove.sort().unique() != scriptApprovalConfig.approvedSignatures.toList().sort().unique()) {
    println "Configuring script-security plugin with approved signatures:\n    ${signaturesToApprove.join('\n    ')}"
    scriptApprovalConfig.setApprovedSignatures(newlyApprovedSignatures as String[])
    println "Successfully configured script-security plugin."
} else {
    println "Nothing changed.  config/script-security-settings.groovy already configured."
}

