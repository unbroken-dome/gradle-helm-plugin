package org.unbrokendome.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade
import org.unbrokendome.gradle.plugins.helm.release.dsl.ChartReference
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.rules.AbstractRule
import org.unbrokendome.gradle.plugins.helm.util.capitalizeWords


/**
 * A rule that creates a [HelmInstallOrUpgrade] task for a release.
 */
class HelmInstallReleaseTaskRule(
        private val tasks: TaskContainer,
        private val releases: NamedDomainObjectContainer<HelmRelease>)
    : AbstractRule() {


    internal companion object {

        const val TaskNamePrefix = "helmInstall"

        fun getTaskName(releaseName: String) =
                TaskNamePrefix + releaseName.capitalizeWords()
    }


    override fun getDescription(): String =
            "Pattern: $TaskNamePrefix<Release>"


    override fun apply(taskName: String) {
        if (taskName.startsWith(TaskNamePrefix)) {

            releases.find { getTaskName(it.name) == taskName }
                    ?.let { release ->

                        tasks.create(taskName, HelmInstallOrUpgrade::class.java) { task ->
                            task.description = "Installs or upgrades the ${release.name} release."

                            task.chart.set(release.chart.map(ChartReference::chartLocation))
                            task.releaseName.set(release.releaseName)
                            task.version.set(release.version)
                            task.repository.set(release.repository)
                            task.namespace.set(release.namespace)
                            task.dryRun.set(release.dryRun)
                            task.atomic.set(release.atomic)
                            task.replace.set(release.replace)
                            task.values.set(release.values)
                            task.valueFiles.from(release.valueFiles)
                            task.wait.set(release.wait)

                            task.dependsOn(HelmPlugin.initServerTaskName)

                            task.dependsOn(TaskDependency {
                                release.chart.orNull
                                        ?.buildDependencies?.getDependencies(it)
                                        ?: emptySet()
                            })

                            // Make sure all releases that this release depends on are installed first
                            task.dependsOn(TaskDependency {
                                release.dependsOn.get()
                                        .mapNotNull { dependencyReleaseName ->
                                            val dependencyTaskName = getTaskName(dependencyReleaseName)
                                            tasks.findByName(dependencyTaskName)
                                        }
                                        .toSet()
                            })
                        }
                    }
        }

    }
}


/**
 * The name of the [HelmInstallOrUpgrade] task associated with this release.
 */
val HelmRelease.installTaskName: String
    get() = HelmInstallReleaseTaskRule.getTaskName(name)
