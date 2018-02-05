/*
   Copyright (c) 2015-2018 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

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
   Configure multiple types of credentials.  Set the `credentials` binding with
   a list of maps containing supported credential types.

   Supported credential types include:
     - BasicSSHUserPrivateKey
     - StringCredentialsImpl
   Example binding:

     credentials = [
         [
             'credential_type': 'BasicSSHUserPrivateKey',
             'credentials_id': 'some-credential-id',
             'description': 'A description of this credential',
             'user': 'some user',
             'key_passwd': 'secret phrase',
             'key': '''
private key contents (do not indent it)
             '''.trim()
         ],
         [
             'credential_type': 'StringCredentialsImpl',
             'credentials_id': 'some-credential-id',
             'description': 'A description of this credential',
             'secret': 'super secret text'
         ],
         [
             'credential_type': 'UsernamePasswordCredentialsImpl',
             'credentials_id': 'some-credential-id',
             'description': 'A description of this credential',
             'user': 'some user',
             'password': 'secret phrase'
         ]
     ]
 */
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

/**
  A shared method used by other "setCredential" methods to safely create a
  credential in the global domain.
  */
def addCredential(String credentials_id, def credential) {
    boolean modified_creds = false
    Domain domain
    SystemCredentialsProvider system_creds = SystemCredentialsProvider.getInstance()
    Map system_creds_map = system_creds.getDomainCredentialsMap()
    domain = (system_creds_map.keySet() as List).find { it.getName() == null }
    if(!system_creds_map[domain] || (system_creds_map[domain].findAll {credentials_id.equals(it.id)}).size() < 1) {
        if(system_creds_map[domain] && system_creds_map[domain].size() > 0) {
            //other credentials exist so should only append
            system_creds_map[domain] << credential
        }
        else {
            system_creds_map[domain] = [credential]
        }
        modified_creds = true
    }
    //save any modified credentials
    if(modified_creds) {
        println "${credentials_id} credentials added to Jenkins."
        system_creds.setDomainCredentialsMap(system_creds_map)
        system_creds.save()
    }
    else {
        println "Nothing changed.  ${credentials_id} credentials already configured."
    }
}

/**
  Supports SSH username and private key (directly entered private key)
  credential provided by BasicSSHUserPrivateKey class.
  Example:

    [
        'credential_type': 'BasicSSHUserPrivateKey',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'user': 'some user',
        'key_passwd': 'secret phrase',
        'key': '''
private key contents (do not indent it)
        '''.trim()
    ]
  */
def setBasicSSHUserPrivateKey(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String user = ((settings['user'])?:'').toString()
    String key = ((settings['key'])?:'').toString()
    String key_passwd = ((settings['key_passwd'])?:'').toString()
    String description = ((settings['description'])?:'').toString()

    addCredential(
            credentials_id,
            new BasicSSHUserPrivateKey(
                CredentialsScope.GLOBAL,
                credentials_id,
                user,
                new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(key),
                key_passwd,
                description)
            )
}

/**
  Supports String credential provided by StringCredentialsImpl class.
  Example:

    [
        'credential_type': 'StringCredentialsImpl',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'secret': 'super secret text'
    ]
  */
def setStringCredentialsImpl(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String description = ((settings['description'])?:'').toString()
    String secret = ((settings['secret'])?:'').toString()
    addCredential(
            credentials_id,
            new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                credentials_id,
                description,
                Secret.fromString(secret))
            )
}

/**
  Supports username and password credential provided by
  UsernamePasswordCredentialsImpl class.

  Example:

    [
        'credential_type': 'UsernamePasswordCredentialsImpl',
        'credentials_id': 'some-credential-id',
        'description': 'A description of this credential',
        'user': 'some user',
        'password': 'secret phrase'
    ]
  */
def setUsernamePasswordCredentialsImpl(Map settings) {
    String credentials_id = ((settings['credentials_id'])?:'').toString()
    String user = ((settings['user'])?:'').toString()
    String password = ((settings['password'])?:'').toString()
    String description = ((settings['description'])?:'').toString()

    addCredential(
            credentials_id,
            new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                credentials_id,
                description,
                user,
                password)
            )
}

if(!binding.hasVariable('credentials')) {
    credentials = []
}
if(!(credentials instanceof List<Map>)) {
    throw new Exception('Error: credentials must be a list of maps.')
}

//iterate through credentials and add them to Jenkins
credentials.each {
    if("set${(it['credential_type'])?:'empty credential_type'}".toString() in this.metaClass.methods*.name.toSet()) {
        "set${it['credential_type']}"(it)
    }
    else {
        println "WARNING: Unknown credential type: ${(it['credential_type'])?:'empty credential_type'}.  Nothing changed."
    }
}

null
