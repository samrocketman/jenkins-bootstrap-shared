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
   Configure job restrictions plugin.  Configre the master node to only allow
   the _jervis_generator job to execute.

   job-restrictions ver 0.6
 */

import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.RegexNameRestriction
import jenkins.model.Jenkins

def restriction = new RegexNameRestriction('_jervis_generator', true)
def prop = new JobRestrictionProperty(restriction)

def j = Jenkins.instance
if(!j.getNodeProperty(JobRestrictionProperty)) {
	j.nodeProperties.add(prop)
	j.save()
	println 'Restricted master to only allow execution from _jervis_generator job.'
}
else {
	println 'Nothing changed.  Master already restricts jobs.'
}
