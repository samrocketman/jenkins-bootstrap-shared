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
   Configure GitHub plugin for GitHub servers and other global Jenkins
   configuration settings.

   github 1.28.0
 */
import net.sf.json.JSONObject
import org.jenkinsci.plugins.github.config.GitHubPluginConfig
import org.jenkinsci.plugins.github.config.GitHubServerConfig
import org.jenkinsci.plugins.github.config.HookSecretConfig

boolean isServerConfigsEqual(List s1, List s2) {
    s1.size() == s2.size() &&
    !(
        false in [s1, s2].transpose().collect { c1, c2 ->
            c1.name == c2.name &&
            c1.apiUrl == c2.apiUrl &&
            c1.manageHooks == c2.manageHooks &&
            c1.credentialsId == c2.credentialsId &&
            c1.clientCacheSize == c2.clientCacheSize
        }
    )
}

boolean isOverrideHookEqual(def global_settings, JSONObject github_plugin) {
    (
        (
            global_settings.isOverrideHookURL() &&
            github_plugin.optString('hookUrl') &&
            global_settings.hookUrl == new URL(github_plugin.optString('hookUrl'))
        ) ||
        (
            !global_settings.isOverrideHookURL() &&
            !github_plugin.optString('hookUrl')
        )
    )
}

boolean isGlobalSettingsEqual(def global_settings, JSONObject github_plugin) {
    global_settings.hookSecretConfig &&
    global_settings.hookSecretConfig.credentialsId == github_plugin.optString('hookSharedSecretId') &&
    isOverrideHookEqual(global_settings, github_plugin)
}

/* Example configuration
github_plugin = [
    //hookUrl: 'http://localhost:8080/github-webhook/',
    hookSharedSecretId: 'webhook-shared-secret',
    servers: [
        'Public GitHub.com': [
            apiUrl: 'https://api.github.com',
            manageHooks: true,
            credentialsId: 'github-token',
        ]
    ]
]
*/

if(!binding.hasVariable('github_plugin')) {
    github_plugin = [:]
}

if(!github_plugin in Map) {
    throw new Exception("github_plugin must be an instance of Map but instead is instance of: ${github_plugin.getClass()}")
}

github_plugin = github_plugin as JSONObject


List configs = []

github_plugin.optJSONObject('servers').each { name, config ->
    if(name && config && config in Map) {
        def server = new GitHubServerConfig(config.optString('credentialsId'))
        server.name = name
        server.apiUrl = config.optString('apiUrl', 'https://api.github.com')
        server.manageHooks = config.optBoolean('manageHooks', false)
        server.clientCacheSize = 20
        //server.clientCacheSize = config.optInt('clientCacheSize', 20)
        configs << server
    }
}

def global_settings = Jenkins.instance.getExtensionList(GitHubPluginConfig.class)[0]

if(github_plugin && (!isGlobalSettingsEqual(global_settings, github_plugin) || !isServerConfigsEqual(global_settings.configs, configs))) {
    if(global_settings.hookSecretConfig && global_settings.hookSecretConfig.credentialsId != github_plugin.optString('hookSharedSecretId')) {
        global_settings.hookSecretConfig = new HookSecretConfig(github_plugin.optString('hookSharedSecretId'))
    }
    if(!isOverrideHookEqual(global_settings, github_plugin)) {
        if(global_settings.isOverrideHookURL() && !github_plugin.optString('hookUrl')) {
            global_settings.overrideHookUrl = false
            global_settings.hookUrl = null
        } else if(global_settings.@hookUrl != new URL(github_plugin.optString('hookUrl'))) {
            global_settings.overrideHookUrl = true
            global_settings.hookUrl = new URL(github_plugin.optString('hookUrl'))
        }
    }
    if(!isServerConfigsEqual(global_settings.configs, configs)) {
        global_settings.configs = configs
    }
    global_settings.save()
    println 'Configured GitHub plugin.'
}
else {
    if(github_plugin) {
        println 'Nothing changed.  GitHub plugin already configured.'
    }
    else {
        println 'Nothing changed.  Skipped configuring GitHub plugin because settings are empty.'
    }
}
