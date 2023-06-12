package com.citi.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.Project
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.rules.chartPackageTaskName
import com.citi.gradle.plugins.helm.rules.chartPackagedArtifactConfigurationName
import com.citi.gradle.plugins.helm.rules.dependenciesConfigurationName
import javax.inject.Inject


/**
 * Used to declare chart dependencies in the DSL.
 */
interface ChartDependencyHandler {

    /**
     * Adds a dependency on another chart.
     *
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @param project the path to the project that contains the chart. If `null`, the dependency will be on
     *        a chart inside the same project as the dependent chart.
     */
    fun add(chart: String = "main", project: String? = null)


    /**
     * Adds a dependency on another chart in the same project.
     *
     * @param chart the [HelmChart] object representing the dependency
     */
    fun add(chart: HelmChart) =
        add(chart = chart.name)


    /**
     * Adds a dependency on another chart, using a map notation.
     *
     * This variant is intended for Groovy DSL support, allowing us to declare a chart dependency like this:
     *
     * ```
     * dependencies {
     *     add(name: 'foo', project: ':bar', chart: 'main')
     * }
     * ```
     *
     * The following keys are supported in the `notation` map parameter:
     *   * `chart`: the name of the chart inside the Gradle project
     *   * `project`: the path to the project that contains the chart
     *
     * @param notation a [Map] containing the dependency properties
     */
    fun add(notation: Map<*, *>) = add(
        chart = notation["chart"]?.toString() ?: "main",
        project = notation["project"]?.toString()
    )
}


private open class DefaultChartDependencyHandler
@Inject constructor(
    private val chart: HelmChart,
    private val project: Project
) : ChartDependencyHandler {


    override fun add(chart: String, project: String?) {

        require(chart != this.chart.name || project != null) { "A chart cannot have a dependency on itself." }

        val dependencyNotation: Any =
            if (project != null) {
                // dependency on a chart in another project
                this.project.dependencies.project(
                    mapOf("path" to project, "configuration" to chartPackagedArtifactConfigurationName(chart))
                )

            } else {
                // dependency on a chart in the same project
                this.project.files(
                    this.project.tasks.named(chartPackageTaskName(chart))
                )
            }

        this.project.dependencies.add(this.chart.dependenciesConfigurationName, dependencyNotation)
    }
}


internal fun createChartDependencyHandler(chart: HelmChart, project: Project): ChartDependencyHandler =
    project.objects.newInstance(DefaultChartDependencyHandler::class.java, chart, project)
