/**
  WARNING SCRIPT IS DEPRECATED.

  Does not work on newer versions of Jenkins.  Recommendation is to stop
  calling it.
  */
// https://www.jenkins.io/doc/book/security/controller-isolation/#agent-controller-access-control

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
