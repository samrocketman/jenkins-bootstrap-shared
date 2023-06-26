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

/**
  This script supports configuring static agents via friendly settings as code.

  Currently supported agents types:

      - JNLP (Java Web Start) agents.  JNLP is an agent type which comes with
        Jenkins Core.
  */

import hudson.model.Node.Mode
import hudson.model.User
import hudson.slaves.DumbSlave
import hudson.slaves.JNLPLauncher
import hudson.slaves.OfflineCause
import jenkins.model.Jenkins
import net.sf.json.JSONObject

//Optional classes which are dynamically loaded
//do not uncomment the following classes; otherwise it will force the class to exist for script to work
//import jenkins.slaves.RemotingWorkDirSettings

//node usage must be NORMAL or EXCLUSIVE
default_node_usage = 'EXCLUSIVE'
default_remote_fs = '/tmp/jenkins-agent'
default_num_executors = 1

/* EXAMPLE AGENT SETTINGS Note: all settings are optional
permanent_agents = [
    "My JNLP Agent": [
        description: "This is my cool agent.",
        executors: 4,
        remote_root_directory: '/tmp/jenkins-agent2',
        labels: "java ruby",
        usage: "normal",
        launcher_java_webstart: [
            tunnel_connection_through: '',
            jvm_options: '',
            disable_workdir: false,
            custom_workdir_path: '',
            internal_data_directory: 'remoting',
            fail_if_workspace_missing: false
        ]
    ]
]
*/

/**
  These methods are necessary for dynamic class loading.  This way, we can
  optionally support agent plugins.  For example, we can support the SSH agent
  plugin without requiring that it be installed if an admin does not need to
  configure SSH agents.

  This script will succeed both with and without the SSH Build Agents plugin
  installed.
 */
Class loadClass(String clazz) {
    try {
        this.class.classLoader.loadClass(clazz)
    }
    catch(ClassNotFoundException e) {
        null
    }
}

List convertParameterTypes(List l) {
    //this method is necessary to handle Java primitives defined in constructors
    l.collect {
        if(it in int) {
            Integer
        }
        else if(it in boolean) {
            Boolean
        }
        else {
            it
        }
    }
}

Boolean compareParameterTypesInListB(List a, List b) {
    if(a.size() == b.size()) {
        !(false in [convertParameterTypes(a), convertParameterTypes(b)].transpose().collect {
            it[0] in it[1]
        })
    }
    else {
        false
    }
}

/**
  Return a new instance of a dynamically loaded class or null.  Useful to
  attempt to instantiate classes on optional plugins and using in logic.
 */
def newClassInstance(String clazz, List argv = []) {
    if(argv) {
        loadClass(clazz)?.getConstructors().find {
            compareParameterTypesInListB(it.parameterTypes.toList(), argv*.class)
        }?.newInstance(*argv)
    }
    else {
        loadClass(clazz)?.newInstance()
    }
}

/**
  Create or update launcher configuration of an agent.  Returns true if created
  or updated.  Returns false if no changes were made.
 */
Boolean createOrUpdateLauncher(DumbSlave agent, String launcher_type, JSONObject launcher_settings) {
    Boolean launcherConfigurationUpdated = false
    def launcher = agent.launcher
    switch(launcher_type) {
        case "launcher_java_webstart":
            if(launcher in JNLPLauncher) {
                //since we are a JNLPLauncher, check launcher settings
                if((launcher.tunnel ?: '') != launcher_settings.optString('tunnel_connection_through')) {
                    launcherConfigurationUpdated = true
                }
                if((launcher.vmargs ?: '') != launcher_settings.optString('jvm_options')) {
                    launcherConfigurationUpdated = true
                }
                //RemotingWorkDirSettings is new since Jenkins 2.72; so defer to dynamic loading instead of assuming the class exists
                if(loadClass('jenkins.slaves.RemotingWorkDirSettings')) {
                    if(launcher.workDirSettings?.disabled != launcher_settings.optBoolean('disable_workdir', false)) {
                        launcherConfigurationUpdated = true
                    }
                    if((launcher.workDirSettings?.workDirPath ?: '') != launcher_settings.optString('custom_workdir_path')) {
                        launcherConfigurationUpdated = true
                    }
                    if(launcher.workDirSettings?.internalDir != (launcher_settings.optString('internal_data_directory', 'remoting') ?: 'remoting')) {
                        launcherConfigurationUpdated = true
                    }
                    if(launcher.workDirSettings?.failIfWorkDirIsMissing != launcher_settings.optBoolean('fail_if_workspace_missing', false)) {
                        launcherConfigurationUpdated = true
                    }
                }
            }
            else {
                //not a JNLPLauncher so we should create it
                launcherConfigurationUpdated = true
            }
            if(launcherConfigurationUpdated) {
                //create a new launcher
                if(loadClass('jenkins.slaves.RemotingWorkDirSettings')) {
                    List workDirSettingsArgs = [
                        launcher_settings.optBoolean('disable_workdir', false),
                        launcher_settings.optString('custom_workdir_path'),
                        launcher_settings.optString('internal_data_directory', 'remoting'),
                        launcher_settings.optBoolean('fail_if_workspace_missing', false)
                    ]
                    launcher = new JNLPLauncher(
                            launcher_settings.optString('tunnel_connection_through'),
                            launcher_settings.optString('jvm_options'),
                            newClassInstance('jenkins.slaves.RemotingWorkDirSettings', workDirSettingsArgs)
                            )
                }
                else {
                    launcher = new JNLPLauncher(launcher_settings.optString('tunnel_connection_through'), launcher_settings.optString('jvm_options'))
                }
            }
            break
        default:
            throw new Exception("Unsupported launcher type specified in configuration -> ${launcher_type}.")
            break
    }
    if(agent.launcher != launcher) {
        agent.computer.disconnect(new OfflineCause.UserCause(User.current(), "Disconnected via Script Console script in order to update settings for ${launcher_type.split('_').join(' ')}."))
        agent.launcher = launcher
        agent.computer.tryReconnect()
    }
    //return true or false
    launcherConfigurationUpdated
}

/**
  Create or update configuration of a permanent agent.  Returns true if created
  or updated.  Returns false if no changes were made.
 */
Boolean createOrUpdateAgent(String name, JSONObject agent_settings) {
    Boolean nodeConfigurationUpdated = false
    Boolean configureNewAgent = false
    def node = Jenkins.instance.getNode(name)
    if(!node || ((node in DumbSlave) && node.remoteFS != agent_settings.optString('remote_root_directory', default_remote_fs))) {
        //create a new permanent agent
        nodeConfigurationUpdated = true
        if(node) {
            node.computer.disconnect(new OfflineCause.UserCause(User.current(), "Disconnected via Script Console script in order to update agent remote root directory."))
        }
        else {
            configureNewAgent = true
        }
        //add a simple default node
        Jenkins.instance.addNode(new DumbSlave(name, agent_settings.optString('remote_root_directory', default_remote_fs), new JNLPLauncher(true)))
        node = Jenkins.instance.getNode(name)
    }
    else {
        if(!(node in DumbSlave)) {
            throw new Exception("ERROR: agent named '${name}' exists but is not a permanent agent.  Aborting as a precaution.")
        }
    }
    //configure other agent settings
    if(node.nodeDescription != agent_settings.optString('description')) {
        node.nodeDescription = agent_settings.optString('description')
        nodeConfigurationUpdated = true
    }
    if(node.numExecutors != agent_settings.optInt('executors', default_num_executors)) {
        node.numExecutors = agent_settings.optInt('executors', default_num_executors)
        nodeConfigurationUpdated = true
    }
    if(node.labelString != agent_settings.optString('labels')) {
        node.labelString = agent_settings.optString('labels')
        nodeConfigurationUpdated = true
    }
    String desired_mode_string = (agent_settings.optString('usage').toUpperCase() in ['NORMAL', 'EXCLUSIVE']) ? agent_settings.optString('usage').toUpperCase() : default_node_usage
    Mode desired_mode = Mode."${desired_mode_string}"
    if(node.mode != desired_mode) {
        node.mode = desired_mode
        nodeConfigurationUpdated = true
    }

    //configure the launcher
    String launcher_type = agent_settings.keySet().toList().find { it.startsWith('launcher_') }
    if(!launcher_type) {
        launcher_type = 'launcher_java_webstart'
    }
    if(createOrUpdateLauncher(node, launcher_type, agent_settings.optJSONObject(launcher_type) ?: ([:] as JSONObject))) {
        nodeConfigurationUpdated = true
    }
    if(nodeConfigurationUpdated) {
        println "${(configureNewAgent) ? 'Created new' : 'Configuration updated for'} agent '${name}'."
    }
    else {
        println "Nothing changed.  Agent '${name}' already configured."
    }
    //return true or false
    nodeConfigurationUpdated
}

/**
  * EXECUTE CONFIGURING AGENTS
  */
if(!binding.hasVariable('permanent_agents')) {
    permanent_agents = [:]
}
if(!(permanent_agents instanceof Map)) {
    throw new Exception('permanent_agents must be a Map.')
}
permanent_agents = permanent_agents as JSONObject
permanent_agents.each { k, v ->
    createOrUpdateAgent(k, v)
}

//do not return a value
null
