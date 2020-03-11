package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.HELM_MAIN_CHART_NAME
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


/**
 * Creates a chart named "main" if no other chart has been created.
 *
 * The chart will be initialized with properties of the project:
 * - chart name: project name
 * - chart version: project version
 * - chart source directory: `src/main/helm`
 */
internal class MainChartRule(
    private val project: Project,
    private val charts: NamedDomainObjectContainer<HelmChart>
) : AbstractRule() {

    override fun getDescription(): String =
        "main chart"


    override fun apply(chartName: String) {
        if (chartName == HELM_MAIN_CHART_NAME && charts.isEmpty()) {
            charts.create(HELM_MAIN_CHART_NAME) { mainChart ->
                mainChart.chartName.set(project.name)
                mainChart.chartVersion.set(project.provider { project.version.toString() })
                mainChart.sourceDir.set(
                    project.layout.projectDirectory.dir("src/main/helm")
                )
            }
        }
    }
}
