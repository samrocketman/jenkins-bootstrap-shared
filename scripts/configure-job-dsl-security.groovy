/*
   Copyright (c) 2015-2020 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Disable Job DSL plugin script Security.  This is disabled because we assume
   users aren't able to create jobs.
 */

import jenkins.model.Jenkins
Jenkins j = Jenkins.instance

if(!j.isQuietingDown()) {
    def job_dsl_security = j.getExtensionList('javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration')[0]
    if(job_dsl_security.useScriptSecurity) {
        job_dsl_security.useScriptSecurity = false
        println 'Job DSL script security has changed.  It is now disabled.'
        job_dsl_security.save()
        j.save()
    }
    else {
        println 'Nothing changed.  Job DSL script security already disabled.'
    }
}
else {
    println 'Shutdown mode enabled.  Configure Job DSL script security SKIPPED.'
}
