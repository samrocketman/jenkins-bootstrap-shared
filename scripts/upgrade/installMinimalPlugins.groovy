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
   Install plugins through the update center.
 */
if(!binding.hasVariable('plugins')) {
    throw new Exception('plugins is missing from the binding.')
}

if(!(plugins instanceof String)) {
    throw new Exception('plugins must defined as a String.')
}

List minimal_plugins = plugins.tokenize('\n')*.trim().sort().unique()


Jenkins.instance.pluginManager.doCheckUpdatesServer()
while(!Jenkins.instance.updateCenter.isSiteDataReady()) {
    sleep(500)
}

println "Installing pinned plugins: ${minimal_plugins.join(', ')}"

// install minimal plugins with no dynamic loading (false)
Jenkins.instance.pluginManager.install(minimal_plugins, false)

null
