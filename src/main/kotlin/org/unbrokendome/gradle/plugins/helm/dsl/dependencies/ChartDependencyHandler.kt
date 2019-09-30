package org.unbrokendome.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.rules.ChartDirArtifactRule
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension
import javax.inject.Inject


/**
 * Used to declare chart dependencies in the DSL.
 */
interface ChartDependencyHandler {

    /**
     * Adds a dependency on another chart.
     *
     * @param name the name of the chart in the dependent chart's requirements. This should either match
     *        the `name` or `alias` field inside the dependent chart's _requirements.yaml_.
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @param project the path to the project that contains the chart. If `null`, the dependency will be on
     *        a chart inside the same project as the dependent chart.
     */
    fun add(name: String, chart: String = "main", project: String? = null)


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
     *   * `name`: the name of the chart in the dependent chart's requirements
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
     * @receiver the name of the chart in the dependent chart's requirements. This should either match
     *           the `name` or `alias` field inside the dependent chart's _requirements.yaml_.
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @param project the path to the project that contains the chart. If `null`, the dependency will be on
     *        a chart inside the same project as the dependent chart.
     */
    operator fun String.invoke(chart: String = "main", project: String? = null) {
        add(this, chart = chart, project = project)
    }
}


private open class DefaultChartDependencyHandler
@Inject constructor(
    private val chart: HelmChart,
    private val project: Project
) : ChartDependencyHandler {

    private val chartDependenciesConfiguration: Configuration
        get() {
            val configurationName = chart.dependenciesConfigurationName
            return project.configurations.run {
                findByName(configurationName) ?: create(configurationName) { configuration ->
                    configuration.isVisible = false

                    // Add a custom extension on the configuration, which is a map of chart names to
                    // objects of type ChartDependency, allowing us to resolve the correct chart artifacts
                    // by chart name.
                    // It would be nicer if we could just store the chart name with each Dependency, but
                    // this is not possible because dependencies within the same project are not realized
                    // as explicit Dependency objects but rather with Configuration.extendsFrom.
                    (configuration as ExtensionAware).extensions.add(
                        MutableMap::class.java,
                        HELM_DEPENDENCIES_CONF_EXTENSION_NAME,
                        mutableMapOf<String, ChartDependency>()
                    )
                }
            }
        }


    override fun add(name: String, chart: String, project: String?) {

        require(chart != this.chart.name || project != null) { "A chart cannot have a dependency on itself." }

        val chartDependency: ChartDependency =
            if (project != null) {
                // dependency on a chart in another project
                val dependency = this.project.dependencies.project(
                    mapOf("path" to project, "configuration" to chartDirArtifactConfigurationName(chart))
                )
                ChartDependency.External(dependency)

            } else {
                // dependency on a chart in the same project
                val artifactConfiguration =
                    this.project.configurations.getByName(chartDirArtifactConfigurationName(chart))
                ChartDependency.Internal(artifactConfiguration)
            }

        chartDependenciesConfiguration.let { configuration ->
            configuration.helmDependencies[name] = chartDependency
            this.project.dependencies.add(configuration.name, chartDependency.dependencyNotation)
        }
    }


    /**
     * Variant of [add] that uses the dynamic method name as dependency name in Groovy.
     */
    @Suppress("unused", "UNCHECKED_CAST")
    fun methodMissing(name: String, arg: Any): Any? {
        val args: Map<String, *> = ((arg as Array<Any?>).firstOrNull() as? Map<String, Any?>?)
            ?: emptyMap<String, Any?>()
        return add(args + mapOf("name" to name))
    }


    private fun chartDirArtifactConfigurationName(dependencyName: String) =
        ChartDirArtifactRule.getConfigurationName(dependencyName)
}


internal fun createChartDependencyHandler(chart: HelmChart, project: Project): ChartDependencyHandler =
    project.objects.newInstance(DefaultChartDependencyHandler::class.java, chart, project)


/**
 * Name of the `helmDependencies` extension on [Configuration] objects.
 */
internal const val HELM_DEPENDENCIES_CONF_EXTENSION_NAME = "helmDependencies"


internal val Configuration.helmDependencies: MutableMap<String, ChartDependency>
    get() = this.requiredExtension(HELM_DEPENDENCIES_CONF_EXTENSION_NAME)
