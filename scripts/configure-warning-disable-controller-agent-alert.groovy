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
/**
  Disable warning when controller has executors.  Controllers with executors
  are safe when the job restrictions plugin is configured.

  https://plugins.jenkins.io/job-restrictions/
  */
import jenkins.model.Jenkins
import jenkins.diagnostics.ControllerExecutorsAgents
import hudson.model.AdministrativeMonitor

def monitor = Jenkins.instance.getExtensionList(AdministrativeMonitor).get(ControllerExecutorsAgents)

if(!monitor.isEnabled()) {
    println('Nothing changed.  Executor on Controllers Warning already disabled')
    return
}

monitor.disable(true)
println('Disabled warning for having executors on the controller.')
