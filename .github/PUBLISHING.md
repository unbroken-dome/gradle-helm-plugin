# Gradle plugin publication

This page describes Gradle Plugin publication to [Gradle Plugin Portal](https://plugins.gradle.org/).

Steps:

1. Receive API Key and Secret from the portal (please
   follow [the documentation](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html)).
2. Navigate to `Actions` and locate
   the [publish workflow](./workflows/publish.yaml). GitHub
   documentation: https://docs.github.com/en/actions/using-workflows/manually-running-a-workflow
3. Insert API Key and Secret from the item '1' above.
4. Create PR with version increment in [gradle.properties](../gradle.properties)
5. Release [the version on GitHub](https://docs.github.com/en/repositories/releasing-projects-on-github).