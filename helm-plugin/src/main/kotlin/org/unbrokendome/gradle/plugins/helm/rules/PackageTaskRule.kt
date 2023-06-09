package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.command.tasks.HelmPackage
import com.citi.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmPackage<Chart>Chart")


internal fun chartPackageTaskName(chartName: String): String =
    namePattern.mapName(chartName)


/**
 * The name of the [HelmPackage] task for this chart.
 */
val HelmChart.packageTaskName
    get() = namePattern.mapName(name)



/**
 * A rule that creates a [HelmPackage] task for each chart.
 */
internal class PackageTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<HelmPackage>(
    HelmPackage::class.java, tasks, charts, namePattern
) {

    override fun HelmPackage.configureFrom(chart: HelmChart) {

        description = "Packages the ${chart.name} chart."

        chartName.set(chart.chartName)
        chartVersion.set(chart.chartVersion)

        sourceDir.set(chart.outputDir)
        updateDependencies.set(false)

        dependsOn(
            chart.updateDependenciesTaskName,
            chart.lintTaskName
        )
    }
}
