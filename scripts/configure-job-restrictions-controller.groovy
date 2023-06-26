/*
   Copyright (c) 2015-2023 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Configure job restrictions plugin.  Configure the controller node to only allow
   the _jervis_generator job to execute.

   Pipeline jobs are not allowed to run on the built-in controller.  However,
   pipelines use the built-in controller to orchestrate nodes, stages, and
   steps.  The WorkflowJob class is a workaround for allowing pipeline jobs to
   orchestrate on the built-in controllver via a flyweight task.  ref:
   https://issues.jenkins-ci.org/browse/JENKINS-31866

   job-restrictions ver 0.6
 */

import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.RegexNameRestriction
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.logic.OrJobRestriction
import io.jenkins.plugins.jobrestrictions.restrictions.job.JobClassNameRestriction
import io.jenkins.plugins.jobrestrictions.util.ClassSelector
import jenkins.branch.OrganizationFolder
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

List<ClassSelector> classList = [
    OrganizationFolder,
    WorkflowMultiBranchProject,
    WorkflowJob
].collect { Class clazz ->
    new ClassSelector(clazz.name)
}
def classes = new JobClassNameRestriction(classList)
def names = new RegexNameRestriction('_jervis_generator|^__.*', true)
def restriction = new OrJobRestriction(names, classes)
def prop = new JobRestrictionProperty(restriction)

def j = Jenkins.instance
if(!j.getNodeProperty(JobRestrictionProperty)) {
	j.nodeProperties.add(prop)
	j.save()
	println 'Restricted built-in controller to only allow execution from _jervis_generator job.'
}
else {
	println 'Nothing changed.  Controller built-in already restricts jobs.'
}
