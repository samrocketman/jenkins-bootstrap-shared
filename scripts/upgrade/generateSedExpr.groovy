/*
   Copyright (c) 2015-2023 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-slack

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
   Generates sed expressions list to be used for updating the build.gradle file.  See also man page sed(1) for -r and -f options.

   Example sed command:

       sed -i.bak -rf sed_expression_file build.gradle
 */

import jenkins.model.Jenkins

//update build.gradle package version string
println "s/^(version ?= ?)'[^']+'(.*)\$/\\1'${Jenkins.instance.version}.1'\\2/"
println "s/^(version)=.*\$/\\1=${Jenkins.instance.version}.1/"
//update jenkins.war version in dependencies.gradle
println "s/(.*getjenkins.*:jenkins-war):[^@]+@(war')/\\1:${Jenkins.instance.version}@\\2/"

//update all plugins in dependencies.gradle
Jenkins.instance.pluginManager.plugins.each { p ->
    println "s/(.*getplugins.*:${p.shortName}):[^@]+@(hpi')/\\1:${p.version}@\\2/"
}
null
