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
   Configure Credentials for docker cloud stack in Jenkins.
   Automatically configure the docker cloud stack in Jenkins.
   Docker plugin v0.16.2
 */

//configure cloud stack

import com.nirima.jenkins.plugins.docker.DockerCloud
import com.nirima.jenkins.plugins.docker.DockerImagePullStrategy
import com.nirima.jenkins.plugins.docker.DockerTemplate
import com.nirima.jenkins.plugins.docker.DockerTemplateBase
import com.nirima.jenkins.plugins.docker.launcher.DockerComputerLauncher
import com.nirima.jenkins.plugins.docker.launcher.DockerComputerSSHLauncher
import com.nirima.jenkins.plugins.docker.strategy.DockerOnceRetentionStrategy
import hudson.model.Node
import hudson.plugins.sshslaves.SSHConnector
import hudson.slaves.RetentionStrategy
import net.sf.json.JSONArray
import net.sf.json.JSONObject

/*
   Docker Clouds defined
*/
JSONArray clouds_docker = [
    [
        name: "docker-local",
        server_url: "http://127.0.0.1:4243",
        container_cap: 10,
        connection_timeout: 5,
        read_timeout: 15,
        credentials_id: "",
        templates: [
            docker_image: "jervis-docker-jvm:latest",
            instance_cap: "4",
            executors: 1,
            remote_fs: "/home/jenkins",
            labels: "stable docker ubuntu1604 sudo language:groovy env jdk",
            //valid values: exclusive or normal
            usage: "exclusive",
            //valid values: pull_latest, pull_always, pull_never
            image_pull_strategy: "pull_never",
            remove_volumes: false,
            //advanced settings
            dns: "",
            docker_command: "/sbin/my_init",
            volumes: "",
            volumes_from: "",
            environment: "",
            lxc_conf_options: "",
            hostname: "",
            memory_limit: -1,
            memory_swap_limit: -1,
            cpu_shares: 0,
            port_bindings: "",
            bind_all_declared_ports: false,
            run_container_privileged: false,
            allocate_pseudo_tty: false,
            mac_address: "",
            remote_fs_root_mapping: "",
            //availability settings
            availability_strategy: "run_once",
            availability_idle_timeout: 5,
            //launcher settings
            launcher_method: "ssh",
            launcher_port: 22,
            launcher_credentials_id: "jenkins-docker-cloud-credentials",
            launcher_jvm_options: "",
            launcher_java_path: "/home/jenkins/java/openjdk8/bin/java",
            launcher_prefix_start_agent_cmd: "",
            launcher_suffix_start_agent_cmd: "",
            launcher_connection_timeout_seconds: 0,
            launcher_max_num_retries: 0,
            launcher_retry_wait_time: 0
        ]

    ]
] as JSONArray


//create a new instance of the DockerCloud class from a JSONObject
def newDockerCloud(JSONObject obj) {
    new DockerCloud(obj.optString('name'),
            bindJSONToList(DockerTemplate.class, obj.opt('templates')),
            obj.optString('server_url'),
            obj.optInt('container_cap', 100),
            obj.optInt('connection_timeout'),
            obj.optInt('read_timeout'),
            obj.optString('credentials_id'),
            "" //don't care about `version` String
            )
}

def newDockerTemplate(JSONObject obj) {
    DockerTemplateBase dockerTemplateBase = new DockerTemplateBase(obj.optString('docker_image'),
            obj.optString('dns'),
            "", //don't care about `network` String
            obj.optString('docker_command'),
            obj.optString('volumes'),
            obj.optString('volumes_from'),
            obj.optString('environment'),
            obj.optString('lxc_conf_options'),
            obj.optString('hostname'),
            obj.optInt('memory_limit') ? obj.optInt('memory_limit') : null,
            obj.optInt('memory_swap_limit') ? obj.optInt('memory_swap_limit') : null,
            obj.optInt('cpu_shares') ? obj.optInt('cpu_shares') : null,
            obj.optString('port_bindings'),
            obj.optBoolean('bind_all_declared_ports', false),
            obj.optBoolean('run_container_privileged', false),
            obj.optBoolean('allocate_pseudo_tty', false),
            obj.optString('mac_address')
            )
    SSHConnector sshConnector = new SSHConnector(obj.optInt('launcher_port', 22),
            obj.optString('launcher_credentials_id'),
            obj.optString('launcher_jvm_options'),
            obj.optString('launcher_java_path'),
            obj.optString('launcher_prefix_start_agent_cmd'),
            obj.optString('launcher_suffix_start_agent_cmd'),
            obj.optInt('launcher_connection_timeout_seconds'),
            obj.optInt('launcher_max_num_retries'),
            obj.optInt('launcher_retry_wait_time')
            )
    //for now the launcher_method will always be "ssh"
    DockerComputerLauncher launcher = new DockerComputerSSHLauncher(sshConnector)
    //this availability_strategy is for "run_once".  We can customize it later
    RetentionStrategy retentionStrategy = new DockerOnceRetentionStrategy(obj.optInt('availability_idle_timeout', 10))
    String node_usage = (obj.optString('usage', 'NORMAL').toUpperCase().equals('EXCLUSIVE'))? 'EXCLUSIVE' : 'NORMAL'
    DockerImagePullStrategy pullStrategy
    switch(obj.optString('image_pull_strategy', 'PULL_LATEST').toUpperCase()) {
        case 'PULL_ALWAYS':
            pullStrategy = DockerImagePullStrategy.PULL_ALWAYS
            break
        case 'PULL_NEVER':
            pullStrategy = DockerImagePullStrategy.PULL_NEVER
            break
        default:
            pullStrategy = DockerImagePullStrategy.PULL_LATEST
    }
    return new DockerTemplate(dockerTemplateBase,
            obj.optString('labels'),
            obj.optString('remote_fs', '/home/jenkins'),
            obj.optString('remote_fs_root_mapping'),
            obj.optString('instance_cap', '1'),
            Node.Mode."${node_usage}",
            obj.optInt('executors', 1),
            launcher,
            retentionStrategy,
            obj.optBoolean('remove_volumes', false),
            pullStrategy
            )
}

def bindJSONToList(Class type, Object src) {
    if(!(type == DockerCloud) && !(type == DockerTemplate)) {
        throw new Exception("Must use DockerCloud or DockerTemplate class.")
    }
    //docker_array should be a DockerCloud or DockerTemplate
    ArrayList<?> docker_array
    if(type == DockerCloud){
        docker_array = new ArrayList<DockerCloud>()
    }
    else {
        docker_array = new ArrayList<DockerTemplate>()
    }
    //cast the configuration object to a Docker instance which Jenkins will use in configuration
    if (src instanceof JSONObject) {
        //uses string interpallation to call a method
        //e.g instead of newDockerCloud(src) we use instead...
        docker_array.add("new${type.getSimpleName()}"(src))
    }
    else if (src instanceof JSONArray) {
        for (Object o : src) {
            if (o instanceof JSONObject) {
                docker_array.add("new${type.getSimpleName()}"(o))
            }
        }
    }
    return docker_array
}

if(!Jenkins.instance.isQuietingDown()) {
    ArrayList<DockerCloud> clouds = new ArrayList<DockerCloud>()
    clouds = bindJSONToList(DockerCloud.class, clouds_docker)
    if(clouds.size() > 0) {
        dockerConfigUpdated = true
        Jenkins.instance.clouds.removeAll(DockerCloud)
        Jenkins.instance.clouds.addAll(clouds)
        clouds*.name.each { cloudName ->
            println "Configured docker cloud ${cloudName}"
        }
        Jenkins.instance.save()
    }
    else {
        println 'Nothing changed.  No docker clouds to configure.'
    }
}
else {
    println 'Shutdown mode enabled.  Configure Docker clouds SKIPPED.'
}

null
