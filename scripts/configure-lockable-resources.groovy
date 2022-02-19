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
  Configures global lockable resources.

   lockable-resources 2.2
 */
import jenkins.model.Jenkins
import org.jenkins.plugins.lockableresources.LockableResource
import org.jenkins.plugins.lockableresources.LockableResourcesManager

if(!binding.hasVariable('lockable_resources')) {
    lockable_resources = []
}
if(!(lockable_resources in List)) {
    throw new Exception('lockable_resources must be a List of Maps.')
}
lockable_resources.each { m ->
    if(!(m in Map)) {
        throw new Exception('lockable_resources must be a List of Maps.')
    }
}

List resources = []

lockable_resources.each { Map r ->
    if(r['name']) {
        resources << new LockableResource(r['name'], r['description']?:'', r['labels']?:'', r['reservedBy']?:'')
    }
}

if(resources) {
    Jenkins j = Jenkins.instance
    LockableResourcesManager resourceManager = j.getExtensionList(LockableResourcesManager.class)[0]
    if(resourceManager.resources != resources) {
        resourceManager.resources = resources
        resourceManager.save()
        println "Configured lockable resources: ${resources*.name.join(', ')}"
    }
    else {
        println 'Nothing changed.  Lockable resources already configured.'
    }
}
