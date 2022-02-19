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
   Exports known credentials from a Jenkins instance.  This exports credentials
   meant to be used by credentials-multitype.groovy.  This makes it quick and
   easy to turn credentials into code.
  */
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

// experimental setting use at your own risk
boolean usePrettyPrint = false

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
        key_passwd: c?.passphrase?.plainText ?: '',
        key: c.privateKey,
        scope: c.scope.toString().toLowerCase()
    ]
}

/**
  * Method to export EC2 credentials for access key ID and access key secret.
  */
Map getAWSCredentialsImpl(def c) {
    [
        credential_type: 'AWSCredentialsImpl',
        credentials_id: c.id,
        description: c.description,
        access_key: c.accessKey ?: '',
        secret_key: c?.secretKey?.plainText ?: '',
        iam_role_arn: c.iamRoleArn ?: '',
        iam_mfa_serial_number: c.iamMfaSerialNumber ?: '',
        scope: c.scope.toString().toLowerCase()
    ]
}

/**
  * Pretty print a groovy inspected object.  Unfortunately, there's no helpful
  * method in Groovy that I'm aware of which makes this easy.
  *
  * Pretty print obj.inspect() output.
  */
String prettyPrint(String groovy_code, boolean usePrettyPrint, String indentChar = '    ') {
    if(!usePrettyPrint) {
        return groovy_code
    }
    int indentLevel = 0
    boolean insideString = false
    boolean escapeChar = false
    String result = ''
    for(int i = 0; i < groovy_code.size(); i++) {
        switch(groovy_code[i]) {
            case ',':
                result += groovy_code[i]
                if(!insideString) {
                    result += '\n'
                    if(indentLevel > 0) {
                        if(groovy_code[i+1] == ']') {
                            if((indentLevel-1) > 0) {
                                result += indentChar * (indentLevel-1)
                            }
                        } else {
                            result += indentChar * indentLevel
                        }
                    }
                }
                break
            case '[':
                result += groovy_code[i]
                if(!insideString) {
                    indentLevel += 1
                    result += '\n'
                    if(indentLevel > 0) {
                        result += indentChar * indentLevel
                    }
                }
                break
            case ']':
                result += groovy_code[i]
                if((i+1) == groovy_code.size()) {
                    result += '\n'
                    break
                }
                if(!insideString) {
                    indentLevel -= 1
                    if(indentLevel < 0) {
                        throw new Exception('Indent level cannot be negative.  This is a bug since you should never see this!')
                    }
                    if(groovy_code[i+1] != ',') {
                        result += '\n'
                        if((indentLevel-1) > 0) {
                            result += indentChar * (indentLevel-1)
                        }
                    }
                }
                break
            case ':':
                result += groovy_code[i]
                if(!insideString) {
                    result += ' '
                }
                break
            case '\'':
                result += groovy_code[i]
                if(escapeChar) {
                    escapeChar = false
                }
                else {
                    insideString = !insideString
                    if(!insideString && !(groovy_code[i+1] in [',', ':'])) {
                        result += '\n'
                        if(indentLevel > 0) {
                            if(groovy_code[i+1] == ']') {
                                if((indentLevel-1) > 0) {
                                    result += indentChar * (indentLevel-1)
                                }
                            } else {
                                result += indentChar * indentLevel
                            }
                        }
                    }
                }
                break
            case '\\':
                escapeChar = !escapeChar
                result += groovy_code[i]
                break
            default:
                if(escapeChar) {
                    escapeChar = false
                }
                result += groovy_code[i]
                break
        }
    }
    result
}

SystemCredentialsProvider system_creds = SystemCredentialsProvider.getInstance()
Map system_creds_map = system_creds.getDomainCredentialsMap()
Domain domain = (system_creds_map.keySet() as List).find { it.getName() == null }

List<Map> credentials_export = []

system_creds_map[domain].each {
    if(!("get${it.class.simpleName}".toString() in this.metaClass.methods*.name.toSet())) {
        return
    }
    credentials_export << "get${it.class.simpleName}"(it)
}

println 'credentials = ' + prettyPrint(credentials_export.inspect(), usePrettyPrint)
