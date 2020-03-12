package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChartInternal
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.chartDependenciesConfigurationName
import org.unbrokendome.gradle.plugins.helm.tasks.HelmCollectChartDependencies


private val namePattern =
    RuleNamePattern.parse("helmCollect<Chart>ChartDependencies")


internal val HelmChart.collectDependenciesTaskName: String
    get() = namePattern.mapName(name)


internal class CollectChartDependenciesTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectContainer<HelmChart>
) : AbstractHelmChartTaskRule<HelmCollectChartDependencies>(
    HelmCollectChartDependencies::class.java, tasks, charts, namePattern
) {

    override fun HelmCollectChartDependencies.configureFrom(chart: HelmChart) {
        dependencies = project.configurations.getByName(chartDependenciesConfigurationName(chart.name))
        outputDir.set(
            (chart as HelmChartInternal).dependenciesDir
        )
    }
}
