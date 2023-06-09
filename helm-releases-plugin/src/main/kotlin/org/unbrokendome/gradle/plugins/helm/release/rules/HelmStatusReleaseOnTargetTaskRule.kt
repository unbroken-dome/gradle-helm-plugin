package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.command.internal.setFrom
import com.citi.gradle.plugins.helm.command.tasks.HelmStatus
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseInternal
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import com.citi.gradle.plugins.helm.release.dsl.shouldInclude
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmStatus<Release>On<Target>")


/**
 * The name of the [HelmStatus] task that checks the status of this release on a given target.
 *
 * @receiver the [HelmRelease]
 * @param targetName the name of the release target
 */
internal fun HelmRelease.statusOnTargetTaskName(targetName: String): String =
    namePattern.mapName(name, targetName)


/**
 * The name of the [HelmStatus] task that checks the status of the given release on this target.
 *
 * @receiver the [HelmReleaseTarget]
 * @param releaseName the name of the release
 */
internal fun HelmReleaseTarget.statusReleaseTaskName(releaseName: String): String =
    namePattern.mapName(releaseName, name)


internal class HelmStatusReleaseOnTargetTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
) : AbstractHelmReleaseToTargetTaskRule<HelmStatus>(
    HelmStatus::class.java, tasks, releases, releaseTargets, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmStatus.configureFrom(release: HelmRelease, releaseTarget: HelmReleaseTarget) {

        description = "Checks the status of the ${release.name} release on the ${releaseTarget.name} target."

        onlyIf {
            releaseTarget.shouldInclude(release)
        }

        val targetSpecific = (release as HelmReleaseInternal).resolveForTarget(releaseTarget)
        setFrom(targetSpecific)

        releaseName.set(targetSpecific.releaseName)
    }
}
