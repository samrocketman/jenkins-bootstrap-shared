/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-slack

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
   Detects if Jenkins needs a restart due to Jenkins core or plugin downloads.
   Returns true if a restart is necessary or false if not.
 */
import hudson.model.UpdateCenter
import hudson.model.UpdateCenter.DownloadJob

UpdateCenter center = Jenkins.instance.updateCenter

println (center.jobs.findAll { it instanceof DownloadJob }.size() as Boolean).toString()
null
