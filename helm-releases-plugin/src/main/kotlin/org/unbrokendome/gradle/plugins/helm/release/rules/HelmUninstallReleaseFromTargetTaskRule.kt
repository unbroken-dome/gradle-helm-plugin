package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.command.internal.setFrom
import com.citi.gradle.plugins.helm.command.tasks.HelmUninstall
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseInternal
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import com.citi.gradle.plugins.helm.release.dsl.shouldInclude
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmUninstall<Release>From<Target>")


/**
 * The name of the [HelmUninstall] task that uninstalls this release from a given target.
 *
 * @receiver the [HelmRelease]
 * @param targetName the name of the release target
 */
internal fun HelmRelease.uninstallFromTargetTaskName(targetName: String): String =
    namePattern.mapName(name, targetName)


/**
 * The name of the [HelmUninstall] task that uninstalls the given release from this target.
 *
 * @receiver the [HelmReleaseTarget]
 * @param releaseName the name of the release
 */
internal fun HelmReleaseTarget.uninstallReleaseTaskName(releaseName: String): String =
    namePattern.mapName(releaseName, name)


internal class HelmUninstallReleaseFromTargetTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
) : AbstractHelmReleaseToTargetTaskRule<HelmUninstall>(
    HelmUninstall::class.java, tasks, releases, releaseTargets, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmUninstall.configureFrom(release: HelmRelease, releaseTarget: HelmReleaseTarget) {

        description = "Uninstalls or upgrades the ${release.name} release from the ${releaseTarget.name} target."

        onlyIf {
            releaseTarget.shouldInclude(release)
        }

        val targetSpecific = (release as HelmReleaseInternal).resolveForTarget(releaseTarget)
        setFrom(targetSpecific)
        releaseName.set(targetSpecific.releaseName)
        keepHistory.set(targetSpecific.keepHistoryOnUninstall)
        wait.set(targetSpecific.wait)

        // Make sure all dependent releases are uninstalled first
        dependsOn(TaskDependency {
            releases
                .matching { otherRelease ->
                    @Suppress("DEPRECATION")
                    otherRelease != release &&
                            release.name in (otherRelease as HelmReleaseInternal)
                        .resolveForTarget(releaseTarget).dependsOn.get()
                }
                .mapNotNull { dependentRelease ->
                    tasks.findByName(releaseTarget.uninstallReleaseTaskName(dependentRelease.name))
                }
                .toSet()
        })

        // Add a mustRunAfter relationship between the tasks for each release that this must be uninstalled after
        mustRunAfter(
            targetSpecific.mustUninstallAfter.map { otherReleaseName ->
                releaseTarget.uninstallReleaseTaskName(otherReleaseName)
            }
        )
    }
}
