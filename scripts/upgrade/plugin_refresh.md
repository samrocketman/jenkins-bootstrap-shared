
# Capture list of plugins

You can capture a full list of plugins before and after plugin refresh to ensure
no important plugins are dropped.

```groovy
println Jenkins.instance.pluginManager.plugins*.shortName.sort().unique().join('\n')
```
