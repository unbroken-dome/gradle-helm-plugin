package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


private val namePattern =
    RuleNamePattern.parse("helmPackage<Chart>Chart")


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
        chartOutputPath

        dependsOn(
            chart.filterSourcesTaskName,
            chart.lintTaskName
        )
    }
}
