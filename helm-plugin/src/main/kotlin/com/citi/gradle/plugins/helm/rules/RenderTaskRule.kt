package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.command.tasks.HelmTemplate
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.HelmChartInternal
import com.citi.gradle.plugins.helm.dsl.HelmRendering
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern = RuleNamePattern2.parse("helmRender<Chart>Chart<Rendering>Rendering")


/**
 * Gets the name of the task that renders a [HelmRendering] from this chart.
 *
 * @param chartName the DSL name of the [HelmChart]
 * @param renderingName the DSL name of the [HelmRendering] within the chart's
 *        [renderings][HelmChart.renderings]
 * @return the render task name
 */
internal fun renderTaskName(chartName: String, renderingName: String): String =
    namePattern.mapName(chartName, renderingName)


/**
 * Gets the name of the task that renders a [HelmRendering] from this chart.
 *
 * @receiver the [HelmChart]
 * @param renderingName the DSL name of the [HelmRendering] within this chart's
 *        [renderings][HelmChart.renderings]
 * @return the render task name
 */
internal fun HelmChart.renderTaskName(renderingName: String): String =
    namePattern.mapName(name, renderingName)


/**
 * A rule that creates a [HelmTemplate] task for each [HelmRendering] in a chart.
 */
internal class RenderTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRuleOuterInner<HelmRendering, HelmTemplate>(
    HelmTemplate::class.java, tasks, charts, { chart -> chart.renderings }, namePattern
) {

    override fun HelmTemplate.configureFrom(chart: HelmChart, innerSource: HelmRendering) {
        description = "Renders the ${innerSource.name} rendering for the ${chart.name} task."
        dependsOn(chart.packageTaskName)

        this.chart.set(chart.packageFile.map { it.asFile.absolutePath })
        this.version.set(chart.chartVersion)
        releaseName.set(innerSource.releaseName)
        values.putAll(innerSource.values)
        fileValues.putAll(innerSource.fileValues)
        valueFiles.from(innerSource.valueFiles)
        apiVersions.set(innerSource.apiVersions)
        isUpgrade.set(innerSource.isUpgrade)
        showOnly.set(innerSource.showOnly)
        validate.set(innerSource.validate)
        useReleaseNameInOutputPath.set(innerSource.useReleaseNameInOutputPath)
        outputDir.set(innerSource.outputDir)

        // Library charts will fail on helm template, so disable the rendering task for them
        onlyIf {
            val chartDescriptor = (chart as HelmChartInternal).chartDescriptor.orNull
            chartDescriptor == null || chartDescriptor.type != "library"
        }
    }
}
