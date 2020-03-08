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


class HelmReleasesPlugin : Plugin<Project> {


    internal companion object {
        const val installAllTaskName = "helmInstall"
        const val uninstallAllTaskName = "helmUninstall"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmPlugin::class.java)

        val releases = createReleasesExtension(project)

        project.tasks.run {
            addRule(HelmInstallReleaseTaskRule(this, releases))
            addRule(HelmUninstallReleaseTaskRule(this, releases))
        }

        createInstallAllReleasesTask(project, releases)
        createUninstallAllReleasesTask(project, releases)
    }


    private fun createReleasesExtension(project: Project) =
        project.helmReleaseContainer()
            .apply {
                (project.helm as ExtensionAware)
                    .extensions.add(HELM_RELEASES_EXTENSION_NAME, this)
            }


    private fun createInstallAllReleasesTask(project: Project, releases: Iterable<HelmRelease>) {
        project.tasks.create(installAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Installs all Helm releases."
            task.dependsOn(allReleasesTaskDependency(project, releases, HelmRelease::installTaskName))
        }
    }


    private fun createUninstallAllReleasesTask(project: Project, releases: Iterable<HelmRelease>) {
        project.tasks.create(uninstallAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Uninstalls all Helm releases."
            task.dependsOn(allReleasesTaskDependency(project, releases, HelmRelease::uninstallTaskName))
        }
    }


    private fun allReleasesTaskDependency(
        project: Project, releases: Iterable<HelmRelease>,
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
