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
   Configures the markup formatter in global security settings to be simple
   HTML.
 */

import hudson.markup.RawHtmlMarkupFormatter

Jenkins j = Jenkins.instance

if(j.markupFormatter.class != RawHtmlMarkupFormatter) {
    j.markupFormatter = new RawHtmlMarkupFormatter(false)
    j.save()
    println 'Markup Formatter configuration has changed.  Configured Markup Formatter.'
}
else {
    println 'Nothing changed.  Markup Formatter already configured.'
}
