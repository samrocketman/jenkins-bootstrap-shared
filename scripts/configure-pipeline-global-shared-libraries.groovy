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
   Configure pipeline shared libraries in the global Jenkins configuration.
   This will safely compare configured libraries and only overwrite the global
   shared library config if changes have been made.

   workflow-cps-global-lib 2.9
 */

import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.traits.BranchDiscoveryTrait
import jenkins.plugins.git.traits.DiscoverOtherRefsTrait
import jenkins.plugins.git.traits.TagDiscoveryTrait
import net.sf.json.JSONObject
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryCachingConfiguration
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever

/**
  Function to compare if the two global shared libraries are equal.
 */
boolean isLibrariesEqual(List lib1, List lib2) {
    //compare returns true or false
    lib1.size() == lib2.size() &&
    !(
        false in [lib1, lib2].transpose().collect { l1, l2 ->
            def c1 = l1.cachingConfiguration
            def c2 = l2.cachingConfiguration
            def s1 = l1.retriever.scm
            def s2 = l2.retriever.scm
            l1.retriever.class == l2.retriever.class &&
            l1.name == l2.name &&
            l1.defaultVersion == l2.defaultVersion &&
            l1.implicit == l2.implicit &&
            l1.allowVersionOverride == l2.allowVersionOverride &&
            l1.includeInChangesets == l2.includeInChangesets &&
            s1.remote == s2.remote &&
            s1.credentialsId == s2.credentialsId &&
            s1.traits.size() == s2.traits.size() &&
            c1?.refreshTimeMinutes == c2?.refreshTimeMinutes &&
            c1?.excludedVersionsStr == c2?.excludedVersionsStr &&
            !(
                false in [s1.traits, s2.traits].transpose().collect { t1, t2 ->
                    t1.class == t2.class && (
                        !(t1 in DiscoverOtherRefsTrait) ||
                        (
                            t1.ref == t2.ref &&
                            t1.nameMapping == t2.nameMapping
                        )
                    )
                }
            )
        }
    )
}

/* Example configuration
pipeline_shared_libraries = [
    'Global Library Name': [
        defaultVersion: 'main',
        implicit: true,
        allowVersionOverride: false,
        includeInChangesets: false,
        cache: [
            refresh_in_minutes: 0,
            exclude_versions: ''
        ],
        scm: [
            remote: 'https://github.com/example/project.git',
            credentialsId: 'your-credentials-id',
            traits: [
               'discoverBranches': true,
               'discoverTags': true,
               'discoverRefs': [
                   [
                       ref: 'pull/123/head',
                       nameMapping: 'pull-123'
                   ]
               ]
            ]
        ]
    ]
]
 */

if(!binding.hasVariable('pipeline_shared_libraries')) {
    pipeline_shared_libraries = [:]
}

if(!pipeline_shared_libraries in Map) {
    throw new Exception("pipeline_shared_libraries must be an instance of Map but instead is instance of: ${pipeline_shared_libraries.getClass()}")
}

pipeline_shared_libraries = pipeline_shared_libraries as JSONObject

List libraries = [] as ArrayList
pipeline_shared_libraries.each { name, config ->
    if(name && config && config in Map && 'scm' in config && config['scm'] in Map && 'remote' in config['scm'] && config['scm'].optString('remote')) {
        def scm = new GitSCMSource(config['scm'].optString('remote'))
        scm.credentialsId = config['scm'].optString('credentialsId')
        List newTraits = []
        if('traits' in config.scm.keySet()) {
            if(config.scm.traits.optBoolean('discoverBranches')) {
                newTraits << (new BranchDiscoveryTrait())
            }
            if(config.scm.traits.optBoolean('discoverTags')) {
                newTraits << (new TagDiscoveryTrait())
            }
            if(config.scm.traits.discoverRefs in List) {
                config.scm.traits.discoverRefs.each { Map refspec ->
                    newTraits << (new DiscoverOtherRefsTrait(refspec.optString('ref'), refspec.optString('nameMapping')))
                }
            }
        } else {
            newTraits = [new BranchDiscoveryTrait()]
        }
        scm.traits = newTraits
        def retriever = new SCMSourceRetriever(scm)
        def library = new LibraryConfiguration(name, retriever)
        library.defaultVersion = config.optString('defaultVersion')
        library.implicit = config.optBoolean('implicit', false)
        library.allowVersionOverride = config.optBoolean('allowVersionOverride', true)
        library.includeInChangesets = config.optBoolean('includeInChangesets', true)
        if(config['cache'] in Map) {
            Integer refresh_time = config.cache.optInt('refresh_in_minutes', 0)
            String exclude_versions = config.cache.optString('exclude_versions', '')
            LibraryCachingConfiguration cachingConfiguration = new LibraryCachingConfiguration(refresh_time, exclude_versions)
            library.cachingConfiguration = cachingConfiguration
        }
        libraries << library
    }
}

def global_settings = Jenkins.instance.getExtensionList(GlobalLibraries.class)[0]

if(libraries && !isLibrariesEqual(global_settings.libraries, libraries)) {
    global_settings.libraries = libraries
    global_settings.save()
    println 'Configured Pipeline Global Shared Libraries:\n    ' + global_settings.libraries.collect { it.name }.join('\n    ')
}
else {
    if(pipeline_shared_libraries) {
        println 'Nothing changed.  Pipeline Global Shared Libraries already configured.'
    }
    else {
        println 'Nothing changed.  Skipped configuring Pipeline Global Shared Libraries because settings are empty.'
    }
}
