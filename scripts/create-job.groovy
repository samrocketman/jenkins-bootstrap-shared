/*
   Copyright (c) 2015-2018 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis

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
   Create Jobs using the script console
   Script should be prepended with the following properties.
     - String itemName
     - String xmlData

    Example Usage:
        curl --data-urlencode "script=String itemName='_jervis_generator';String xmlData='''$(<./configs/job_jervis_config.xml)''';$(<./scripts/create-job.groovy)" http://localhost:8080/scriptText
 */

import hudson.model.FreeStyleProject
import hudson.model.Item
import jenkins.model.Jenkins
import jenkins.model.ModifiableTopLevelItemGroup

boolean isPropertiesSet = false

try {
    //just by trying to access properties should throw an exception
    itemName == null
    xmlData == null
    isPropertiesSet = true
} catch(MissingPropertyException e) {
    println 'ERROR Can\'t create job.'
    println 'ERROR Missing properties: itemName, xmlData'
}

List<PluginWrapper> plugins = Jenkins.instance.pluginManager.getPlugins()
//get a list of installed plugins
Set<String> installed_plugins = []
plugins.each {
    installed_plugins << it.getShortName()
}

Set<String> required_plugins = []

if((required_plugins-installed_plugins).size() == 0) {
    if(isPropertiesSet) {
        Jenkins instance = Jenkins.getInstance()
        ModifiableTopLevelItemGroup itemgroup = instance
        int i = itemName.lastIndexOf('/')
        if(i > 0) {
            String group = itemName.substring(0, i)
            Item item = instance.getItemByFullName(group)
            if (item instanceof ModifiableTopLevelItemGroup) {
                itemgroup = (ModifiableTopLevelItemGroup) item
            }
            itemName = itemName.substring(i + 1)
        }
        Jenkins.checkGoodName(itemName)
        if(itemgroup.getJob(itemName) == null) {
            println "Created job \"${itemName}\"."
            itemgroup.createProjectFromXML(itemName, new ByteArrayInputStream(xmlData.getBytes()))
            instance.save()
        } else {
            println "Nothing changed.  Job \"${itemName}\" already exists."
        }
    }
} else {
    println 'Unable to create job.'
    println "Missing required plugins: ${required_plugins-installed_plugins}"
}
