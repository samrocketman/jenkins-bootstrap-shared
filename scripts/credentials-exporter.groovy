/*
   Copyright (c) 2015-2019 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Exports known credentials from a Jenkins instance.
  */
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

/**
  * Method to export username and password credentials.
  */
Map getUsernamePasswordCredentialsImpl(UsernamePasswordCredentialsImpl c) {
  [
    credential_type: 'UsernamePasswordCredentialsImpl',
    credentials_id: c.id,
    description: c.description,
    user: c.username,
    password: c.password.plainText,
    scope: c.scope.toString().toLowerCase()
  ]
}

/**
  * Method to export String credentials typically used for API tokens.
  */
Map getStringCredentialsImpl(StringCredentialsImpl c) {
  [
    credential_type: 'StringCredentialsImpl',
    credentials_id: c.id,
    description: c.description,
    secret: c.secret.plainText,
    scope: c.scope.toString().toLowerCase()
  ]
}

/**
  * Method to export user and SSH private key credentials.
  */
Map getBasicSSHUserPrivateKey(BasicSSHUserPrivateKey c) {
  [
    credential_type: 'BasicSSHUserPrivateKey',
    credentials_id: c.id,
    description: c.description,
    user: c.username,
    key_passwd: c.passphrase.plainText,
    key: c.privateKey
  ]
}


SystemCredentialsProvider system_creds = SystemCredentialsProvider.getInstance()
Map system_creds_map = system_creds.getDomainCredentialsMap()
Domain domain = (system_creds_map.keySet() as List).find { it.getName() == null }

List<Map> credentials_export = []

system_creds_map[domain].each {
  if(!(it.class.simpleName in ['UsernamePasswordCredentialsImpl', 'StringCredentialsImpl', 'BasicSSHUserPrivateKey'])) {
    return
  }
  credentials_export << "get${it.class.simpleName}"(it)
}
println 'credentials = ' + credentials_export.inspect()
