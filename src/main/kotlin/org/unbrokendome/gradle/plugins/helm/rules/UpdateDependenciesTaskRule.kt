package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildOrUpdateDependencies
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


private val namePattern =
    RuleNamePattern.parse("helmUpdate<Chart>ChartDependencies")


/**
 * The name of the [HelmBuildOrUpdateDependencies] task for this chart.
 */
val HelmChart.updateDependenciesTaskName
    get() = namePattern.mapName(name)



/**
 * A rule that creates a [HelmBuildOrUpdateDependencies] task for each chart.
 */
internal class BuildDependenciesTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<HelmBuildOrUpdateDependencies>(
    HelmBuildOrUpdateDependencies::class.java, tasks, charts, namePattern
) {

    override fun HelmBuildOrUpdateDependencies.configureFrom(chart: HelmChart) {

        description = "Builds or updates the dependencies for the ${chart.name} chart."

        chartDir.set(chart.outputDir)

        dependsOn(
            HelmPlugin.addRepositoriesTaskName,
            chart.filterSourcesTaskName
        )
    }
}
