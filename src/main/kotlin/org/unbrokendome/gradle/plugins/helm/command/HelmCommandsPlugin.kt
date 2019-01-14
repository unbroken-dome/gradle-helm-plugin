package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.HELM_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.dsl.createHelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.createLinting
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty


class HelmCommandsPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        val helmExtension = createHelmExtension(project)
        project.extensions.add(HelmExtension::class.java, HELM_EXTENSION_NAME, helmExtension)


        createLinting(project.objects)
                .apply {
                    enabled.set(
                            project.booleanProviderFromProjectProperty("helm.lint.enabled", true))
                    strict.set(
                            project.booleanProviderFromProjectProperty("helm.lint.strict"))

                    (helmExtension as ExtensionAware).extensions
                            .add(Linting::class.java, HELM_LINT_EXTENSION_NAME, this)
                }
    }
}
