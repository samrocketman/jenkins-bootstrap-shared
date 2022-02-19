/*
   Copyright 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-slack

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
   Gets a list of pinned plugins and only prints the plugins which have been
   pinned as files.  This is useful for determining a minimal set of plugins
   installed vs plugins installed provided as dependencies.
 */
import jenkins.model.Jenkins
println Jenkins.instance.pluginManager.plugins.findAll {
  new File("${Jenkins.instance.root}/plugins/${it.shortName}.jpi.pinned").exists()
}*.shortName.sort().unique().join('\n')
