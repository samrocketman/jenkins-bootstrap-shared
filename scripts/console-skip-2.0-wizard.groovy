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
   Skip the Jenkins 2.0 wizard.  We already configure Jenkins to our liking during
   bootstrap.
 */
import hudson.util.PluginServletFilter
def j=Jenkins.instance

legacySetupWizard = ('getSetupWizard' in j.metaClass.methods*.name)
newSetupWizard = (('getInstallState' in j.metaClass.methods*.name) && ('isSetupComplete' in j.installState.metaClass.methods*.name))


if((!newSetupWizard && legacySetupWizard) || (newSetupWizard && !j.installState.isSetupComplete())) {
    def w=j.setupWizard
    if(w != null) {
        try {
          //pre Jenkins 2.6
          w.completeSetup(j)
          PluginServletFilter.removeFilter(w.FORCE_SETUP_WIZARD_FILTER)
        }
        catch(Exception e) {
          w.completeSetup()
        }
        j.save()
        println 'Jenkins 2.0 wizard skipped.'
    }
}
