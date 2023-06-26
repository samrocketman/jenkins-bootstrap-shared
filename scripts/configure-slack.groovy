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
   Configure slack plugin global settings.
 */
import jenkins.model.Jenkins

if(!binding.hasVariable('slack_settings')) {
    slack_settings = [:]
}
if(!(slack_settings instanceof Map)) {
    throw new Exception('slack_settings must be a Map.')
}

def slack = Jenkins.instance.getExtensionList('jenkins.plugins.slack.SlackNotifier$DescriptorImpl')[0]

boolean save = false
String teamDomain = (slack_settings['teamDomain'])?:''
String tokenCredentialId = (slack_settings['tokenCredentialId'])?:''
String room = (slack_settings['room'])?:''

if(teamDomain != slack.teamDomain) {
    slack.teamDomain = teamDomain
    save = true
}
if(tokenCredentialId != slack.tokenCredentialId) {
    slack.tokenCredentialId = tokenCredentialId
    save = true
}
if(room != slack.room) {
    slack.room = room
    save = true
}
if(save) {
    println 'Slack configured.'
    slack.save()
}
else {
    println 'Nothing changed.  Slack already configured.'
}
