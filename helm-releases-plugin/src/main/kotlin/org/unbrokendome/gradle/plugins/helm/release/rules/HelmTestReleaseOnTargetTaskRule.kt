package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.command.internal.setFrom
import com.citi.gradle.plugins.helm.command.tasks.HelmTest
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseInternal
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import com.citi.gradle.plugins.helm.release.dsl.shouldInclude
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmTest<Release>On<Target>")


/**
 *
 * The name of the [HelmTest] task that tests this release on a given target.
 *
 * @receiver the [HelmRelease]
 * @param targetName the name of the release target
 */
internal fun HelmRelease.testOnTargetTaskName(targetName: String): String =
    namePattern.mapName(name, targetName)


/**
 * The name of the [HelmTest] task that tests the given release on this target.
 *
 * @receiver the [HelmReleaseTarget]
 * @param releaseName the name of the release
 */
internal fun HelmReleaseTarget.testReleaseTaskName(releaseName: String): String =
    namePattern.mapName(releaseName, name)


internal class HelmTestReleaseOnTargetTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
) : AbstractHelmReleaseToTargetTaskRule<HelmTest>(
    HelmTest::class.java, tasks, releases, releaseTargets, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmTest.configureFrom(release: HelmRelease, releaseTarget: HelmReleaseTarget) {

        description = "Tests the ${release.name} release on the ${releaseTarget.name} target."

        val targetSpecific = (release as HelmReleaseInternal).resolveForTarget(releaseTarget)

        onlyIf {
            releaseTarget.shouldInclude(release)
        }
        onlyIf {
            targetSpecific.test.enabled.get()
        }

        mustRunAfter(release.installToTargetTaskName(releaseTarget.name))

        releaseName.set(release.releaseName)
        setFrom(targetSpecific)
        showLogs.set(targetSpecific.test.showLogs)
        remoteTimeout.set(targetSpecific.test.timeout)
    }
}
