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
   Configure a single LDAP server in the LDAP plugin security realm.

   ldap 1.16
 */

import hudson.security.LDAPSecurityRealm
import hudson.util.Secret
import jenkins.model.IdStrategy
import jenkins.security.plugins.ldap.LDAPConfiguration
import net.sf.json.JSONObject

if(!binding.hasVariable('ldap_settings')) {
    ldap_settings = [:]
}
if(!(ldap_settings instanceof Map)) {
    throw new Exception('ldap_settings must be a Map.')
}

ldap_settings = ldap_settings as JSONObject

if(!(Jenkins.instance.securityRealm instanceof LDAPSecurityRealm)) {
	LDAPConfiguration conf = new LDAPConfiguration(
            ldap_settings.optString('server'),
            ldap_settings.optString('rootDN'),
            ldap_settings.optBoolean('inhibitInferRootDN'),
            ldap_settings.optString('managerDN'),
            Secret.fromString(ldap_settings.optString('managerPasswordSecret')))
    conf.userSearchBase = ldap_settings.optString('userSearchBase')
	conf.userSearch = ldap_settings.optString('userSearch', LDAPSecurityRealm.DescriptorImpl.DEFAULT_USER_SEARCH)
	conf.groupSearchBase = ldap_settings.optString('groupSearchBase')
	conf.groupSearchFilter = ldap_settings.optString('groupSearchFilter')
	conf.environmentProperties = (ldap_settings.opt('environmentProperties')?:[:]).collect { k, v ->
        new LDAPSecurityRealm.EnvironmentProperty(k.toString(), v.toString())
    } as LDAPSecurityRealm.EnvironmentProperty[]
	conf.displayNameAttributeName = ldap_settings.optString('displayNameAttributeName', LDAPSecurityRealm.DescriptorImpl.DEFAULT_DISPLAYNAME_ATTRIBUTE_NAME)
	conf.mailAddressAttributeName = ldap_settings.optString('mailAddressAttributeName', LDAPSecurityRealm.DescriptorImpl.DEFAULT_MAILADDRESS_ATTRIBUTE_NAME)

	List<LDAPConfiguration> configurations = [conf]
	Jenkins.instance.securityRealm = new LDAPSecurityRealm(
            configurations,
            ldap_settings.optBoolean('disableMailAddressResolver'),
            null,
            IdStrategy.CASE_INSENSITIVE,
            IdStrategy.CASE_INSENSITIVE)
    Jenkins.instance.save()
    println 'Security realm set to LDAP.'
}
else {
    println 'Nothing changed.  LDAP security realm already configured.'
}
