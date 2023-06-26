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
   Configure repositories in which @Grab will search in global shared libraries
   for pipelines.  This will configure ~/.groovy/grapeConfig.xml
 */

import java.security.MessageDigest
import jenkins.model.Jenkins

ivyXml = '''
<!--
Modified from default settings adding Maven Central:
https://github.com/apache/groovy/blob/master/src/resources/groovy/grape/defaultGrapeConfig.xml

See also:
http://docs.groovy-lang.org/latest/html/documentation/grape.html#Grape-CustomizeIvysettings
-->
<ivysettings>
  <settings defaultResolver="downloadGrapes"/>
  <resolvers>
    <chain name="downloadGrapes" returnFirst="true">
      <filesystem name="cachedGrapes">
        <ivy pattern="${user.home}/.groovy/grapes/[organisation]/[module]/ivy-[revision].xml"/>
        <artifact pattern="${user.home}/.groovy/grapes/[organisation]/[module]/[type]s/[artifact]-[revision](-[classifier]).[ext]"/>
      </filesystem>
      <ibiblio name="localm2" root="file:${user.home}/.m2/repository/" checkmodified="true" changingPattern=".*" changingMatcher="regexp" m2compatible="true"/>
      <!-- todo add 'endorsed groovy extensions' resolver here -->
      <ibiblio name="mavencentral" root="https://repo1.maven.org/maven2" m2compatible="true"/>
      <ibiblio name="jcenter" root="https://jcenter.bintray.com/" m2compatible="true"/>
      <ibiblio name="ibiblio" m2compatible="true"/>
    </chain>
  </resolvers>
</ivysettings>
'''.trim()

//method for calculating sha256sum
sha256sum = { input ->
    MessageDigest.getInstance('SHA-256').digest(input.bytes).encodeHex().toString()
}

//method for determining grapeConfig equality
boolean isGrapeConfigEqual(File xmlFile, xml) {
    String xmlChecksum = sha256sum((xml + "\n").toString())
    //compare equality
    xmlFile.exists() &&
    sha256sum(xmlFile) == xmlChecksum
}

ivyXmlFile = new File("${(System.getenv('HOME'))?:Jenkins.instance.root}/.groovy/grapeConfig.xml")
if(!ivyXmlFile.parentFile.exists()) {
    ivyXmlFile.getParentFile().mkdirs()
}
if(!isGrapeConfigEqual(ivyXmlFile, ivyXml)) {
    ivyXmlFile.withWriter { f ->
        f.write(ivyXml)
        //add newline to end for POSIX compatibility
        f.write("\n")
    }
    println 'Configured Grape repository config used by @Grab.'
}
else {
    println 'Nothing changed.  Grape repository config used by @Grab already configured.'
}
