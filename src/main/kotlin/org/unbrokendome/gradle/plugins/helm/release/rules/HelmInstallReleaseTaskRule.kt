package org.unbrokendome.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade
import org.unbrokendome.gradle.plugins.helm.release.dsl.ChartReference
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmInstall<Release>")


/**
 * The name of the [HelmInstallOrUpgrade] task associated with this release.
 */
val HelmRelease.installTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates a [HelmInstallOrUpgrade] task for a release.
 */
internal class HelmInstallReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectContainer<HelmRelease>
) : AbstractHelmReleaseTaskRule<HelmInstallOrUpgrade>(
    HelmInstallOrUpgrade::class.java, tasks, releases, namePattern
) {

    override fun HelmInstallOrUpgrade.configureFrom(release: HelmRelease) {
        description = "Installs or upgrades the ${release.name} release."

        chart.set(release.chart.map(ChartReference::chartLocation))
        releaseName.set(release.releaseName)
        version.set(release.version)
        repository.set(release.repository)
        namespace.set(release.namespace)
        dryRun.set(release.dryRun)
        atomic.set(release.atomic)
        replace.set(release.replace)
        values.set(release.values)
        fileValues.set(release.fileValues)
        valueFiles.from(release.valueFiles)
        wait.set(release.wait)

        dependsOn(TaskDependency {
            release.chart.orNull
                ?.buildDependencies?.getDependencies(it)
                ?: emptySet()
        })

        // Make sure all releases that this release depends on are installed first
        dependsOn(TaskDependency {
            release.dependsOn.get()
                .mapNotNull { dependencyReleaseName ->
                    val dependencyTaskName = namePattern.mapName(dependencyReleaseName)
                    tasks.findByName(dependencyTaskName)
                }
                .toSet()
        })
    }
}
