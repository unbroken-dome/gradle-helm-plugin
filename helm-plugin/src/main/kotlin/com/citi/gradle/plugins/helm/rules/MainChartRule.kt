package com.citi.gradle.plugins.helm.rules

import com.citi.gradle.plugins.helm.HELM_MAIN_CHART_NAME
import com.citi.gradle.plugins.helm.dsl.HelmChart
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Rule


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
) : Rule {

    override fun getDescription(): String =
        "main chart"


    override fun apply(chartName: String) {
        if (chartName == HELM_MAIN_CHART_NAME && charts.isEmpty()) {

            if (project.file("src/main/helm/Chart.yaml").isFile) {
                charts.create(HELM_MAIN_CHART_NAME) { mainChart ->
                    mainChart.chartName.convention(project.name)
                    mainChart.sourceDir.convention(
                        project.layout.projectDirectory.dir("src/main/helm")
                    )
                }
            }
        }
    }
}
