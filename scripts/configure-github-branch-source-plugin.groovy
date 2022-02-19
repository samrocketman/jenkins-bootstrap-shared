/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis

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
   Configure API endpoints in the GitHub Branch Source plugin global
   configuration.

   github-branch-source 2.2.3
 */
import org.jenkinsci.plugins.github_branch_source.GitHubConfiguration
import org.jenkinsci.plugins.github_branch_source.Endpoint

boolean isEndpointsEqual(List<Endpoint> e1, List<Endpoint> e2) {
    e1.size() == e2.size() &&
    !(
        false in [e1, e2].transpose().collect { a, b ->
            a.name == b.name &&
            a.apiUri == b.apiUri
        }
    )
}

/* Example configuration
github_branch_source = [
    'https://github.example.com/api/v3': 'Public GitHub API'
]
*/

if(!binding.hasVariable('github_branch_source')) {
    github_branch_source = [:]
}
if(!github_branch_source in Map) {
    throw new Exception("github_branch_source must be an instance of Map but instead is instance of: ${github_branch_source.getClass()}")
}

List endpoints = github_branch_source.collect { k, v -> new Endpoint(k, v) }
def global_settings = Jenkins.instance.getExtensionList(GitHubConfiguration.class)[0]

if(!isEndpointsEqual(global_settings.endpoints, endpoints)) {
    global_settings.endpoints = endpoints
    println 'Configured GitHub Branch Source plugin.'
}
else {
    println 'Nothing changed.  GitHub Branch Source plugin already configured.'
}
