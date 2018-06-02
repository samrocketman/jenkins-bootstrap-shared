/*
   Copyright (c) 2015-2018 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-jervis

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

  - None TODO: configure different agent types
  */

/**
  These methods are necessary for dynamic class loading.  This way, we can
  optionally support agent plugins.  For example, we can support the SSH agent
  plugin without requiring that it be installed if an admin does not need to
  configure SSH agents.

  This script will succeed both with and without the SSH slaves plugin
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
        true in [convertParameterTypes(a), convertParameterTypes(b)].transpose().collect {
            !(it[0] in it[1])
        }
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

//If ssh slaves plugin is installed, then it will return a new instance.
//If ssh slaves plugin is not installed, then it will return null.
newClassInstance('hudson.plugins.sshslaves.SSHLauncher', ['localhost', 22, '', '', '', '', '', (int) 60, 0, 0, newClassInstance('hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy')])
