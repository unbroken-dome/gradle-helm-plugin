package org.unbrokendome.gradle.plugins.helm.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.release.dsl.helmReleaseContainer
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmInstallReleaseTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmUninstallReleaseTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.installTaskName
import org.unbrokendome.gradle.plugins.helm.release.rules.uninstallTaskName
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty


class HelmReleasesPlugin : Plugin<Project> {


    internal companion object {
        const val installAllTaskName = "helmInstall"
        const val uninstallAllTaskName = "helmUninstall"
    }


    override fun apply(project: Project) {

        project.run {

            project.plugins.apply(HelmPlugin::class.java)

            val releases = createReleasesExtension()

            tasks.run {
                addRule(HelmInstallReleaseTaskRule(this, releases))
                addRule(HelmUninstallReleaseTaskRule(this, releases))
            }

            createInstallAllReleasesTask(releases)
            createUninstallAllReleasesTask(releases)
        }
    }


    private fun Project.createReleasesExtension() =
        project.helmReleaseContainer()
            .also { releases ->
                (project.helm as ExtensionAware)
                    .extensions.add(HELM_RELEASES_EXTENSION_NAME, releases)

                releases.all { release ->
                    release.dryRun.convention(
                        booleanProviderFromProjectProperty("helm.dryRun")
                    )

                    release.atomic.convention(
                        project.booleanProviderFromProjectProperty("helm.atomic")
                    )
                }
            }


    private fun Project.createInstallAllReleasesTask(releases: Iterable<HelmRelease>) {
        tasks.register(installAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Installs all Helm releases."
            task.dependsOn(allReleasesTaskDependency(releases, HelmRelease::installTaskName))
        }
    }


    private fun Project.createUninstallAllReleasesTask(releases: Iterable<HelmRelease>) {
        tasks.register(uninstallAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Uninstalls all Helm releases."
            task.dependsOn(allReleasesTaskDependency(releases, HelmRelease::uninstallTaskName))
        }
    }


    private fun Project.allReleasesTaskDependency(
        releases: Iterable<HelmRelease>,
        taskNameFn: (HelmRelease) -> String
    ) =
        TaskDependency {
            releases
                .map { release ->
                    val taskName = taskNameFn(release)
                    project.tasks.getByName(taskName)
                }
                .toSet()
        }
}
