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
   Configures pipeline flow durability to speed up the time it takes in which
   pipelines execute.  This is a recommended performance optimization by the
   Jenkins community.
 */
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint
import org.jenkinsci.plugins.workflow.flow.GlobalDefaultFlowDurabilityLevel

Jenkins.instance.getExtensionList(GlobalDefaultFlowDurabilityLevel.DescriptorImpl).first().with {
    if(it.durabilityHint != FlowDurabilityHint.PERFORMANCE_OPTIMIZED) {
        it.durabilityHint = FlowDurabilityHint.PERFORMANCE_OPTIMIZED
        it.save()
        println 'Configured flow durability to be performance optimized.'
    } else {
        println 'Nothing changed.  Flow durability already performance optimized.'
    }
}
