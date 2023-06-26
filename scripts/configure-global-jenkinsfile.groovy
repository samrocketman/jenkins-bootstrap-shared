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
   Configure a global Jenkinsfile within the Config File Provider plugin.  This
   is meant to be used by the Pipeline Multibranch Defaults Plugin.
 */

import jenkins.model.Jenkins
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles
import org.jenkinsci.plugins.configfiles.groovy.GroovyScript
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage

//bindings
script_approval = Jenkins.instance.getExtensionList('org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval')[0]
config_files = Jenkins.instance.getExtensionList(GlobalConfigFiles)[0]

if(!binding.hasVariable('jenkinsfile_script')) {
    jenkinsfile_script = ''
}
if(!(jenkinsfile_script instanceof String)) {
    throw new Exception('jenkinsfile_script must be a String.')
}

jenkinsfile_script = jenkinsfile_script.trim()

if(!jenkinsfile_script) {
    return
}

if(!config_files.getById('Jenkinsfile') || (config_files.getById('Jenkinsfile').content != jenkinsfile_script)) {
	//create global config
	GroovyScript new_config = new GroovyScript('Jenkinsfile', 'Global Jenkinsfile', '', jenkinsfile_script)
	config_files.save(new_config)
	println 'Configured global Jenkinsfile.'
}
else {
    println 'Nothing changed.  Global Jenkinsfile already configured.'
}

//approve Jenkinsfile script for execution
String hash = script_approval.DEFAULT_HASHER.hash(jenkinsfile_script, GroovyLanguage.get().getName())
if(hash in script_approval.approvedScriptHashes) {
	println 'Nothing changed.  Global Jenkinsfile script already approved.'
}
else {
    script_approval.approveScript(hash)
    script_approval.save()
	println 'Global Jenkinsfile script has been approved in script security.'
}
