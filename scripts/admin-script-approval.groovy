/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis

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
   Automatically approve the post-build groovy script defined in the
   maintenance and seed jobs which are created by admins.

   This is the same thing as an admin approving the script so it will be run.
 */

import hudson.model.FreeStyleProject
import hudson.model.Item
import hudson.plugins.groovy.SystemGroovy
import jenkins.model.Jenkins
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildDescriptor

script_approval = Jenkins.get().getExtensionList('org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval')[0]

approvalsNotUpdated = true

void approveGroovyScript(String fullName, String script, String type = '') {
    String hash = script_approval.hash(script, 'groovy')
    if(!(hash in script_approval.approvedScriptHashes)) {
        println "${fullName} ${(type)? type + ' ' : ''}groovy script approved."
        script_approval.approveScript(hash)
        approvalsNotUpdated = false
    }
}

//find admin maintenance and seed jobs
Jenkins.get().items.findAll { Item i ->
    (i in FreeStyleProject) && i.fullName.startsWith('_')
}.each { FreeStyleProject j ->
    //approve groovy script builders (system groovy script build step)
    j.builders.findAll {
        (it in SystemGroovy) && it.source?.script?.script
    }*.source.script.script.each {
        approveGroovyScript(j.fullName, it, 'build step system')
    }
    //approve groovy script publishers (groovy postbuild step)
    j.publishers.findAll { k, v ->
        (k in GroovyPostbuildDescriptor) && v?.script?.script
    }.collect { k, v ->
        v.script.script
    }.each {
        approveGroovyScript(j.fullName, it, 'postbuild')
    }
}

if(approvalsNotUpdated) {
    println 'Nothing changed.  Admin groovy scripts already approved.'
}

null
