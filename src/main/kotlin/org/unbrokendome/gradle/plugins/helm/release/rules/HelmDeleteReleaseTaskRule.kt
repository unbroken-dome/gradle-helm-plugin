package org.unbrokendome.gradle.plugins.helm.release.rules

import com.vdurmont.semver4j.Semver
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmDelete
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmVersion
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.rules.AbstractRule
import org.unbrokendome.gradle.plugins.helm.util.capitalizeWords


/**
 * A rule that creates a [HelmDelete] task for a release.
 */
internal class HelmDeleteReleaseTaskRule(
    private val tasks: TaskContainer,
    private val releases: NamedDomainObjectContainer<HelmRelease>
) : AbstractRule() {


    internal companion object {

        const val TaskNamePrefix = "helmDelete"

        fun getTaskName(releaseName: String) =
            TaskNamePrefix + releaseName.capitalizeWords()
    }


    override fun getDescription(): String =
        "Pattern: $TaskNamePrefix<Release>"


    override fun apply(taskName: String) {
        if (taskName.startsWith(TaskNamePrefix)) {

            releases.find { getTaskName(it.name) == taskName }
                ?.let { release ->

                    tasks.create(taskName, HelmDelete::class.java) { task ->
                        task.description = "Deletes the ${release.name} release."

                        task.releaseName.set(release.releaseName)
                        task.dryRun.set(release.dryRun)
                        task.purge.set(
                                task.project.provider<Boolean> {
                                    val versionTask: HelmVersion =
                                            this.tasks.findByName(HelmPlugin.clientVersionTaskName) as HelmVersion
                                    if(versionTask.clientVersion.isGreaterThanOrEqualTo(Semver("3.0.0"))){
                                        false
                                    } else {
                                        release.purge.get()
                                    }
                                }
                        )

                        task.namespace.set(
                                task.project.provider<String> {
                                    val versionTask: HelmVersion =
                                            this.tasks.findByName(HelmPlugin.clientVersionTaskName) as HelmVersion
                                    if(versionTask.clientVersion.isGreaterThanOrEqualTo(Semver("3.0.0"))){
                                        release.namespace.orNull
                                    } else {
                                        null
                                    }
                                }
                        )

                        task.dependsOn(HelmPlugin.initServerTaskName, HelmPlugin.clientVersionTaskName)

                        // Make sure all dependent releases are deleted first
                        task.dependsOn(TaskDependency {
                            releases
                                .matching { otherRelease ->
                                    otherRelease != release &&
                                            release.name in otherRelease.dependsOn.get()
                                }
                                .mapNotNull { dependentRelease ->
                                    tasks.findByName(dependentRelease.deleteTaskName)
                                }
                                .toSet()
                        })
                    }
                }
        }
    }
}


/**
 * The name of the [HelmDelete] task associated with this release.
 */
val HelmRelease.deleteTaskName: String
    get() = HelmDeleteReleaseTaskRule.getTaskName(name)
