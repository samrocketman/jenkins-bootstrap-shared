//https://wiki.jenkins-ci.org/display/JENKINS/Slave+To+Master+Access+Control

def rule = Jenkins.instance.getExtensionList(jenkins.security.s2m.MasterKillSwitchConfiguration.class)[0].rule
if(!rule.getMasterKillSwitch()) {
    rule.setMasterKillSwitch(true)
    //dismiss the warning because we don't care (cobertura reporting is broken otherwise)
    Jenkins.instance.getExtensionList(jenkins.security.s2m.MasterKillSwitchWarning.class)[0].disable(true)
    Jenkins.instance.save()
    println 'Disabled agent -> built-in controller security for cobertura.'
}
else {
    println 'Nothing changed.  Agent -> built-in controller security already disabled.'
}
