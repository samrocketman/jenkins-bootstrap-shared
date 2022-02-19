
/*
   Copyright (c) 2015-2022 Sam Gleske - https://github.com/samrocketman/jenkins-bootstrap-shared

   Permission is hereby granted, free of charge, to any person obtaining a copy of
   this software and associated documentation files (the "Software"), to deal in
   the Software without restriction, including without limitation the rights to
   use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
   the Software, and to permit persons to whom the Software is furnished to do so,
   subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/*
   Configure the content security policy to be less strict so reports can be
   loaded.  This is necessary for reporting tools which embed unsafe inline
   JavaScript and CSS.

   Note: if this is undesirable, then you can override this script to disable
   it in a local bootstrapper.  Example:

       mkdir scripts/init.groovy.d
       touch scripts/init.groovy.d/set-content-security-policy.groovy

   By having an empty file in your local bootstrapper, this script will get
   overwritten by it and never execute.  Alternatively, you could copy this
   script to your local bootstrapper and customize the content security policy
   yourself.

   See also https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy
*/

import jenkins.model.Jenkins

System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "default-src 'self' ${(Jenkins.instance.rootUrl ?: 'http://localhost:8080') -~ '/$'} 'unsafe-inline'")
