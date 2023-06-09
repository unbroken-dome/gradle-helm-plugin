package com.citi.gradle.plugins.helm.release.rules

import com.citi.gradle.plugins.helm.command.internal.mergeValues
import com.citi.gradle.plugins.helm.command.internal.setFrom
import com.citi.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseInternal
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import com.citi.gradle.plugins.helm.release.dsl.shouldInclude
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmInstall<Release>To<Target>")


/**
 * The name of the [HelmInstallOrUpgrade] task that installs this release to a given target.
 *
 * @receiver the [HelmRelease]
 * @param targetName the name of the release target
 */
internal fun HelmRelease.installToTargetTaskName(targetName: String): String =
    namePattern.mapName(name, targetName)


/**
 * The name of the [HelmInstallOrUpgrade] task that installs the given release to this target.
 *
 * @receiver the [HelmReleaseTarget]
 * @param releaseName the name of the release
 */
internal fun HelmReleaseTarget.installReleaseTaskName(releaseName: String): String =
    namePattern.mapName(releaseName, name)


internal class HelmInstallReleaseToTargetTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
) : AbstractHelmReleaseToTargetTaskRule<HelmInstallOrUpgrade>(
    HelmInstallOrUpgrade::class.java, tasks, releases, releaseTargets, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmInstallOrUpgrade.configureFrom(release: HelmRelease, releaseTarget: HelmReleaseTarget) {

        description = "Installs or upgrades the ${release.name} release to the ${releaseTarget.name} target."

        onlyIf {
            releaseTarget.shouldInclude(release)
        }

        val targetSpecific = (release as HelmReleaseInternal).resolveForTarget(releaseTarget)
        setFrom(targetSpecific)
        mergeValues(targetSpecific)
        releaseName.set(targetSpecific.releaseName)
        chart.set(targetSpecific.chart.map { it.chartLocation })
        version.set(targetSpecific.version)
        replace.set(targetSpecific.replace)
        historyMax.set(targetSpecific.historyMax)

        dependsOn(targetSpecific.chart)

        dependsOn.addAll(targetSpecific.installDependsOn)

        // Make sure all releases that this release depends on are installed first
        @Suppress("DEPRECATION")
        dependsOn(
            targetSpecific.dependsOn.map { releaseDependencies ->
                releaseDependencies.map { dependencyName ->
                    releaseTarget.installReleaseTaskName(dependencyName)
                }
            }
        )

        // Add a mustRunAfter relationship between the tasks for each release that this must be installed after
        mustRunAfter(
            targetSpecific.mustInstallAfter.map { otherReleaseName ->
                releaseTarget.installReleaseTaskName(otherReleaseName)
            }
        )
    }
}
