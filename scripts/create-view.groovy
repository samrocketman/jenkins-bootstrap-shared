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
   Create views using the script console
   Script should be prepended with the following properties.
     - String itemName
     - String xmlData

    Example Usage:
        curl --data-urlencode "script=String itemName='Welcome';String xmlData='''$(<./configs/view_welcome_config.xml)''';$(<./scripts/create-view.groovy)" http://localhost:8080/scriptText
 */

import hudson.model.View
import javax.xml.transform.stream.StreamSource
import jenkins.model.Jenkins

boolean isPropertiesSet = false

try {
    //just by trying to access properties should throw an exception
    itemName == null
    xmlData == null
    isPropertiesSet = true
} catch(MissingPropertyException e) {
    println 'ERROR Can\'t create view.'
    println 'ERROR Missing properties: itemName, xmlData'
}

List<PluginWrapper> plugins = Jenkins.instance.pluginManager.getPlugins()
//get a list of installed plugins
Set<String> installed_plugins = []
plugins.each {
    installed_plugins << it.getShortName()
}

Set<String> required_plugins = ['dashboard-view', 'view-job-filters']

if((required_plugins-installed_plugins).size() == 0) {
    if(isPropertiesSet) {
        Jenkins instance = Jenkins.getInstance()
        View newView = View.createViewFromXML(itemName, new ByteArrayInputStream(xmlData.getBytes()))
        Jenkins.checkGoodName(itemName)
        if(instance.getView(itemName) == null) {
            println "Created view \"${itemName}\"."
            instance.addView(newView)
            instance.save()
        } else {
            instance.getView(itemName).updateByXml(new StreamSource(new ByteArrayInputStream(xmlData.getBytes())))
            instance.getView(itemName).save()
            println "View \"${itemName}\" already exists.  Updated using XML."
        }
    }
}
