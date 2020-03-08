package org.unbrokendome.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUninstall
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.rules.AbstractRule
import org.unbrokendome.gradle.plugins.helm.util.capitalizeWords


/**
 * A rule that creates a [HelmUninstall] task for a release.
 */
internal class HelmUninstallReleaseTaskRule(
    private val tasks: TaskContainer,
    private val releases: NamedDomainObjectContainer<HelmRelease>
) : AbstractRule() {


    internal companion object {

        const val TaskNamePrefix = "helmUninstall"

        fun getTaskName(releaseName: String) =
            TaskNamePrefix + releaseName.capitalizeWords()
    }


    override fun getDescription(): String =
        "Pattern: $TaskNamePrefix<Release>"


    override fun apply(taskName: String) {
        if (taskName.startsWith(TaskNamePrefix)) {

            releases.find { getTaskName(it.name) == taskName }
                ?.let { release ->

                    tasks.create(taskName, HelmUninstall::class.java) { task ->
                        task.description = "Uninstalls the ${release.name} release."

                        task.releaseName.set(release.releaseName)
                        task.dryRun.set(release.dryRun)
                        task.keepHistory.set(release.keepHistoryOnUninstall)

                        // Make sure all dependent releases are uninstalled first
                        task.dependsOn(TaskDependency {
                            releases
                                .matching { otherRelease ->
                                    otherRelease != release &&
                                            release.name in otherRelease.dependsOn.get()
                                }
                                .mapNotNull { dependentRelease ->
                                    tasks.findByName(dependentRelease.uninstallTaskName)
                                }
                                .toSet()
                        })
                    }
                }
        }
    }
}


/**
 * The name of the [HelmUninstall] task associated with this release.
 */
val HelmRelease.uninstallTaskName: String
    get() = HelmUninstallReleaseTaskRule.getTaskName(name)
