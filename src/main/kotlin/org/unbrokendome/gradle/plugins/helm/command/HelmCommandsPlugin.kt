package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmCommandTask
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmInstallationCommandTask
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmServerCommandTask
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmServerOperationCommandTask
import org.unbrokendome.gradle.plugins.helm.dsl.HelmDownloadClient
import org.unbrokendome.gradle.plugins.helm.dsl.HelmDownloadClientInternal
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.dsl.createHelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.createLinting
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.durationProviderFromProjectProperty


class HelmCommandsPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        val helmExtension = project.createHelmExtension()
        project.extensions.add(HelmExtension::class.java, HELM_EXTENSION_NAME, helmExtension)

        project.objects.createLinting()
            .apply {
                enabled.convention(
                    project.booleanProviderFromProjectProperty("helm.lint.enabled", defaultValue = true)
                )
                strict.convention(
                    project.booleanProviderFromProjectProperty("helm.lint.strict")
                )

                (helmExtension as ExtensionAware).extensions
                    .add(Linting::class.java, HELM_LINT_EXTENSION_NAME, this)
            }


        // Apply the global Helm options as defaults to each command task
        project.tasks.withType(AbstractHelmCommandTask::class.java) { task ->
            task.globalOptions.set(helmExtension)

            task.downloadedExecutable.set(
                (helmExtension.downloadClient as HelmDownloadClientInternal).executable.map { it.asFile.absolutePath }
            )

            // Depend on the helmExtractClient task, but only if it's configured to download & extract the client
            task.dependsOn(TaskDependency {
                val hasLocalExecutable = (it as? AbstractHelmCommandTask?)?.localExecutable?.isPresent == true
                if (!hasLocalExecutable && helmExtension.downloadClient.enabled.get()) {
                    setOf(project.tasks.getByName(HelmDownloadClient.HELM_EXTRACT_CLIENT_TASK_NAME))
                } else emptySet()
            })
        }

        project.tasks.withType(AbstractHelmServerCommandTask::class.java) { task ->
            task.conventionsFrom(helmExtension as ConfigurableHelmServerOptions)
        }

        project.tasks.withType(AbstractHelmServerOperationCommandTask::class.java) { task ->
            task.dryRun.convention(
                project.booleanProviderFromProjectProperty("helm.dryRun")
            )
            task.noHooks.convention(
                project.booleanProviderFromProjectProperty("helm.noHooks")
            )
            task.remoteTimeout.convention(
                project.durationProviderFromProjectProperty("helm.remoteTimeout")
            )
        }

        project.tasks.withType(AbstractHelmInstallationCommandTask::class.java) { task ->
            task.atomic.convention(
                project.booleanProviderFromProjectProperty("helm.atomic")
            )
            task.wait.convention(
                project.booleanProviderFromProjectProperty("helm.wait")
            )
        }
    }
}
