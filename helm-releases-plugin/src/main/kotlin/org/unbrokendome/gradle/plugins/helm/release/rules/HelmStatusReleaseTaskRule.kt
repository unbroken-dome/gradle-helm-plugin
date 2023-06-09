package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.command.tasks.HelmStatus
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmStatus<Release>")


/**
 * The name of the [HelmStatus] task associated with this release.
 */
val HelmRelease.statusTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates a task to install a release to the active target.
 */
internal class HelmStatusReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    private val activeTargetName: Provider<String>
) : AbstractHelmReleaseTaskRule<Task>(
    Task::class.java, tasks, releases, namePattern
) {

    override fun Task.configureFrom(release: HelmRelease) {
        group = HELM_GROUP
        description = "Checks the status of the ${release.name} release on the active target."

        dependsOn(
            activeTargetName.map { release.statusOnTargetTaskName(it) }
        )
    }
}
