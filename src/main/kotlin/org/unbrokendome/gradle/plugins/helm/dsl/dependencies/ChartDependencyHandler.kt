package org.unbrokendome.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.rules.chartPackageTaskName
import org.unbrokendome.gradle.plugins.helm.rules.chartPackagedArtifactConfigurationName
import org.unbrokendome.gradle.plugins.helm.rules.dependenciesConfigurationName
import javax.inject.Inject


/**
 * Used to declare chart dependencies in the DSL.
 */
interface ChartDependencyHandler {

    /**
     * Adds a dependency on another chart.
     *
     * @param name the name that the subchart will have inside the parent chart
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @param project the path to the project that contains the chart. If `null`, the dependency will be on
     *        a chart inside the same project as the dependent chart.
     */
    fun add(name: String, chart: String = "main", project: String? = null)


    /**
     * Adds a dependency on another chart in the same project.
     *
     * @param name the name that the subchart will have inside the parent chart
     * @param chart the [HelmChart] object representing the dependency
     */
    @JvmDefault
    fun add(name: String, chart: HelmChart) =
        add(name, chart = chart.name)


    /**
     * Adds a dependency on another chart in the same project. The subchart will have
     * its own [HelmChart.chartName] inside the parent chart.
     *
     * @param chart the [HelmChart] object representing the dependency
     */
    @JvmDefault
    fun add(chart: HelmChart) =
        add("", chart = chart.name)


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
     *   * `name`: the name that the subchart will have in the resulting chart
     *   * `chart`: the name of the chart inside the Gradle project
     *   * `project`: the path to the project that contains the chart
     *
     * @param notation a [Map] containing the dependency properties
     */
    @JvmDefault
    fun add(notation: Map<*, *>) = add(
        name = requireNotNull(notation["name"]?.toString()) {
            "The \"name\" parameter is required when declaring a chart dependency."
        },
        chart = notation["chart"]?.toString() ?: "main",
        project = notation["project"]?.toString()
    )


    /**
     * Adds a dependency on another chart.
     *
     * This variant is intended for Kotlin DSL support, allowing us to declare a chart dependency like this:
     *
     * ```
     * dependencies {
     *     "foo"(project = ":bar", chart = "main")
     * }
     * ```
     *
     * @receiver the name that the subchart will have inside the parent chart
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @param project the path to the project that contains the chart. If `null`, the dependency will be on
     *        a chart inside the same project as the dependent chart.
     */
    operator fun String.invoke(chart: String = "main", project: String? = null) {
        add(this, chart = chart, project = project)
    }


    /**
     * Adds a dependency on another chart.
     *
     * This variant is intended for Kotlin DSL support, allowing us to declare a chart dependency like this:
     *
     * ```
     * dependencies {
     *     "foo"(barChart)
     * }
     * ```
     *
     * @receiver the name that the subchart will have inside the parent chart
     * @param chart the [HelmChart] object representing the dependency
     */
    operator fun String.invoke(chart: HelmChart) {
        add(this, chart)
    }
}


private open class DefaultChartDependencyHandler
@Inject constructor(
    private val chart: HelmChart,
    private val project: Project
) : ChartDependencyHandler {

    override fun add(name: String, chart: String, project: String?) {

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


    /**
     * Variant of [add] that uses the dynamic method name as dependency name in Groovy.
     *
     * @param name the name of the method called; interpreted as the name of the dependency
     * @param arg any additional arguments
     */
    @Suppress("unused", "UNCHECKED_CAST")
    fun methodMissing(name: String, arg: Any): Any? =
        when (val firstArg = (arg as Array<Any?>).firstOrNull()) {
            is HelmChart -> add(name, firstArg)
            is Map<*, *> -> add(firstArg as Map<String, Any?> + mapOf("name" to name))
            else -> add(mapOf("name" to name))
        }
}


internal fun createChartDependencyHandler(chart: HelmChart, project: Project): ChartDependencyHandler =
    project.objects.newInstance(DefaultChartDependencyHandler::class.java, chart, project)
