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
   Disable submitting anonymous usage statistics to the Jenkins project.
   An option like this shouldn't be enabled by default.
 */

import jenkins.model.Jenkins

def j = Jenkins.instance
if(!j.isQuietingDown()) {
    if(j.isUsageStatisticsCollected()){
        j.setNoUsageStatistics(true)
        j.save()
        println 'Disabled submitting usage stats to Jenkins project.'
    }
    else {
        println 'Nothing changed.  Usage stats are not submitted to the Jenkins project.'
    }
}
else {
    println 'Shutdown mode enabled.  Disable usage stats SKIPPED.'
}
