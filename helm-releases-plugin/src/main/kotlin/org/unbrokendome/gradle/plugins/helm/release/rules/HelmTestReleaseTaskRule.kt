package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.command.tasks.HelmTest
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmTest<Release>")


/**
 * The name of the [HelmTest] task associated with this release.
 */
val HelmRelease.testTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates a task to test a release on the active target.
 */
internal class HelmTestReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    private val activeTargetName: Provider<String>
) : AbstractHelmReleaseTaskRule<Task>(
    Task::class.java, tasks, releases, namePattern
) {

    override fun Task.configureFrom(release: HelmRelease) {
        group = HELM_GROUP
        description = "Tests the ${release.name} release on the active target."

        dependsOn(
            activeTargetName.map { release.testOnTargetTaskName(it) }
        )
    }
}
