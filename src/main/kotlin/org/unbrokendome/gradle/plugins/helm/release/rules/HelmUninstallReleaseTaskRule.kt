package org.unbrokendome.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUninstall
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmUninstall<Release>")


/**
 * The name of the [HelmUninstall] task associated with this release.
 */
val HelmRelease.uninstallTaskName: String
    get() = namePattern.mapName(name)



/**
 * A rule that creates a [HelmUninstall] task for a release.
 */
internal class HelmUninstallReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectContainer<HelmRelease>
) : AbstractHelmReleaseTaskRule<HelmUninstall>(
    HelmUninstall::class.java, tasks, releases, namePattern
) {

    override fun HelmUninstall.configureFrom(release: HelmRelease) {

        description = "Uninstalls the ${release.name} release."

        releaseName.set(release.releaseName)
        dryRun.set(release.dryRun)
        keepHistory.set(release.keepHistoryOnUninstall)

        // Make sure all dependent releases are uninstalled first
        dependsOn(TaskDependency {
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
