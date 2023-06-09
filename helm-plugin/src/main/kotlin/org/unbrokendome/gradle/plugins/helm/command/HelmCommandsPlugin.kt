package com.citi.gradle.plugins.helm.command

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.HELM_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import com.citi.gradle.plugins.helm.command.internal.conventionsFrom
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmCommandTask
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmInstallationCommandTask
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmServerCommandTask
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmServerOperationCommandTask
import com.citi.gradle.plugins.helm.dsl.*
import org.unbrokendome.gradle.pluginutils.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.durationProviderFromProjectProperty


class HelmCommandsPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        val helmExtension = project.createHelmExtension()
        project.extensions.add(HelmExtension::class.java, HELM_EXTENSION_NAME, helmExtension)

        // Install the HelmDownloadClientPlugin on the root project, this allows us to sync the download
        // between multiple subprojects that need the Helm client
        project.rootProject.pluginManager.apply(HelmDownloadClientPlugin::class.java)

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
        val downloadClient = helmExtension.downloadClient as HelmDownloadClientInternal

        project.tasks.withType(AbstractHelmCommandTask::class.java) { task ->
            task.globalOptions.set(helmExtension)

            task.dependsOn(TaskDependency {
                setOfNotNull(downloadClient.extractClientTask.orNull)
            })

            task.downloadedExecutable.set(
                downloadClient.executable.map { it.asFile.absolutePath }
            )
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
            task.waitForJobs.convention(
                project.booleanProviderFromProjectProperty("helm.waitForJobs")
            )
        }
    }
}
