package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.HelmPlugin
import com.citi.gradle.plugins.helm.command.tasks.HelmUpdateDependencies
import com.citi.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmUpdate<Chart>ChartDependencies")


/**
 * The name of the [HelmUpdateDependencies] task for this chart.
 */
val HelmChart.updateDependenciesTaskName
    get() = namePattern.mapName(name)



/**
 * A rule that creates a [HelmUpdateDependencies] task for each chart.
 */
internal class UpdateDependenciesTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<HelmUpdateDependencies>(
    HelmUpdateDependencies::class.java, tasks, charts, namePattern
) {

    override fun HelmUpdateDependencies.configureFrom(chart: HelmChart) {

        description = "Builds or updates the dependencies for the ${chart.name} chart."

        chartDir.set(chart.outputDir)

        // We depend on the update repositories task (which will cache the repo index for some time),
        // so no need to refresh again
        skipRefresh.set(true)

        dependsOn(
            HelmPlugin.updateRepositoriesTaskName,
            chart.collectChartSourcesTaskName
        )
    }
}
