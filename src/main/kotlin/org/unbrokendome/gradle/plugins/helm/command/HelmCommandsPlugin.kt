package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.HELM_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmCommandTask
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmServerCommandTask
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.dsl.createHelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.createLinting
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty


class HelmCommandsPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        val helmExtension = project.createHelmExtension()
        project.extensions.add(HelmExtension::class.java, HELM_EXTENSION_NAME, helmExtension)


        project.objects.createLinting()
            .apply {
                enabled.set(
                    project.booleanProviderFromProjectProperty("helm.lint.enabled", true)
                )
                strict.set(
                    project.booleanProviderFromProjectProperty("helm.lint.strict")
                )

                (helmExtension as ExtensionAware).extensions
                    .add(Linting::class.java, HELM_LINT_EXTENSION_NAME, this)
            }


        // Apply the global Helm options as defaults to each command task
        project.tasks.withType(AbstractHelmCommandTask::class.java) { task ->
            task.run {
                executable.set(helmExtension.executable)
                debug.set(helmExtension.debug)
                extraArgs.addAll(helmExtension.extraArgs)
                xdgDataHome.set(helmExtension.xdgDataHome)
                xdgConfigHome.set(helmExtension.xdgConfigHome)
                xdgCacheHome.set(helmExtension.xdgCacheHome)
                registryConfigFile.set(helmExtension.registryConfigFile)
                repositoryCacheDir.set(helmExtension.repositoryCacheDir)
                repositoryConfigFile.set(helmExtension.repositoryConfigFile)
            }
        }

        project.tasks.withType(AbstractHelmServerCommandTask::class.java) { task ->
            task.run {
                kubeConfig.set(helmExtension.kubeConfig)
                kubeContext.set(helmExtension.kubeContext)
                remoteTimeout.set(helmExtension.remoteTimeout)
                namespace.set(helmExtension.namespace)
            }
        }
    }
}
