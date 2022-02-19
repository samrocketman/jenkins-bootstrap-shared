/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Configure matrix authorization strategy with permissions for users and
   groups.  This script is idempotent and will only change configuration if
   necessary.

   Example configuration:
       authz_strategy_config = [
           strategy: 'GlobalMatrixAuthorizationStrategy',
           user_permissions: [
               anonymous: ['Job Discover'],
               authenticated: ['Overall Read', 'Job Read', 'View Read'],
               admin: ['Overall Administer']
           ]
       ]

   Available Authorization Strategies:
       GlobalMatrixAuthorizationStrategy
       ProjectMatrixAuthorizationStrategy

   Available user permissions:
       Overall Administer
       Overall Read
       Agent Configure
       Agent Delete
       Agent Create
       Agent Disconnect
       Agent Connect
       Agent Build
       Agent Provision
       Run Delete
       Run Update
       Job Create
       Job Delete
       Job Configure
       Job Read
       Job Discover
       Job Build
       Job Workspace
       Job Cancel
       SCM Tag
       Credentials Create
       Credentials Update
       Credentials View
       Credentials Delete
       Credentials ManageDomains
       Job Move
       View Create
       View Delete
       View Configure
       View Read
       Run Replay

   "Job ViewStatus" permission becomes available after installing the
   embeddable build status plugin.
 */

import hudson.security.GlobalMatrixAuthorizationStrategy
import hudson.security.Permission
import hudson.security.ProjectMatrixAuthorizationStrategy
import jenkins.model.Jenkins

/**
 * FUNCTIONS AND SETUP CODE
 */
String shortName(Permission p) {
    p.id.tokenize('.')[-2..-1].join(' ')
        .replace('Hudson','Overall')
        .replace('Computer', 'Agent')
        .replace('Item', 'Job')
        .replace('CredentialsProvider', 'Credentials')
}

Map getCurrentPermissions(Map config = [:]) {
    Map currentPermissions = [:].withDefault { [].toSet() }
    if(!('getGrantedPermissions' in Jenkins.instance.authorizationStrategy.metaClass.methods*.name.sort().unique())) {
        return currentPermissions
    }
    Closure merger = { Map nmap, Map m ->
        m.each { k, v ->
            nmap[k] += v
        }
    }
    Jenkins.instance.authorizationStrategy.grantedPermissions.collect { permission, userList ->
        userList.collect { user ->
            [ (user): shortName(permission) ]
        }
    }.flatten().each merger.curry(currentPermissions)
    currentPermissions
}

boolean isConfigurationEqual(Map config) {
    Map currentPermissions = getCurrentPermissions(config)
    Jenkins.instance.authorizationStrategy.class.name.endsWith(config['strategy']) &&
    !(false in config['user_permissions'].collect { k, v -> currentPermissions[k] == v.toSet() }) &&
    currentPermissions.keySet() == config['user_permissions'].keySet()
}

boolean isValidConfig(def config, List<String> validPermissions) {
    Map currentPermissions = getCurrentPermissions()
    config instanceof Map &&
    config.keySet().containsAll(['strategy', 'user_permissions']) &&
    config['strategy'] &&
    config['strategy'] instanceof String &&
    config['strategy'] in ['GlobalMatrixAuthorizationStrategy', 'ProjectMatrixAuthorizationStrategy'] &&
    config['user_permissions'] &&
    !(false in config['user_permissions'].collect { k, v ->
        k instanceof String &&
        (v instanceof List || v instanceof Set) &&
        !(false in v.collect {
            validPermissions.contains(it)
        })
    })
}

Map<String, Permission> permissionIds = Permission.all.findAll { permission ->
    List<String> nonConfigurablePerms = ['RunScripts', 'UploadPlugins', 'ConfigureUpdateCenter']
    permission.enabled &&
        !permission.id.startsWith('hudson.security.Permission') &&
        !(true in nonConfigurablePerms.collect { permission.id.endsWith(it) })
}.collect { permission ->
    [ (shortName(permission)): permission ]
}.sum()

/**
 * MAIN EXECUTION
 */

if(!binding.hasVariable('authz_strategy_config')) {
    authz_strategy_config = [:]
}

if(!isValidConfig(authz_strategy_config, permissionIds.keySet().toList())) {
    println([
        'Skip configuring matrix authorization strategy because no valid config was provided.',
        'Available Authorization Strategies:\n    GlobalMatrixAuthorizationStrategy\n    ProjectMatrixAuthorizationStrategy',
        "Available Permissions:\n    ${permissionIds.keySet().join('\n    ')}"
    ].join('\n'))
    return
}

if(isConfigurationEqual(authz_strategy_config)) {
    println "Nothing changed.  ${authz_strategy_config['strategy']} authorization strategy already configured."
    return
}

println "Configuring authorization strategy ${authz_strategy_config['strategy']}"

def authz_strategy = Class.forName("hudson.security.${authz_strategy_config['strategy']}").newInstance()

// build the permissions in the strategy
authz_strategy_config['user_permissions'].each { user, permissions ->
    permissions.each { p ->
        authz_strategy.add(permissionIds[p], user)
        println "    For user ${user} grant permission ${p}."
    }
}

// configure global authorization
Jenkins.instance.authorizationStrategy = authz_strategy

// save settings to persist across restarts
Jenkins.instance.save()
