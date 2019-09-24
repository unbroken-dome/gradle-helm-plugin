package org.unbrokendome.gradle.plugins.helm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.ChartDependenciesResolver
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.chartDependenciesConfigurationName
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.model.ChartRequirementsYaml
import org.unbrokendome.gradle.plugins.helm.util.*
import java.io.File
import java.io.Reader
import java.io.StringReader
import java.util.*


val FilteredFilePatterns = listOf("Chart.yaml", "values.yaml", "requirements.yaml")


/**
 * Processes the Helm chart sources and copies them into an intermediate directory.
 *
 * This task has several purposes:
 * - apply a filtering transformation (i.e. placeholder resolution) transformation on certain
 *   source files, as specified by the `filtering` property. For example, the placeholder
 *   `${chartVersion} in the _Chart.yaml_ file will be replaced by the actual `chartVersion`
 *   value.
 * - copy the chart source files into an intermediate directory that has the same name as
 *   the chart, as is required by the `helm package` command.
 */
@Suppress("LeakingThis")
open class HelmFilterSources : DefaultTask() {

    init {
        group = HELM_GROUP
    }


    /**
     * The name of the chart within the `helm.charts` DSL container.
     */
    @get:[Input Optional]
    val configuredChartName: Property<String> =
            project.objects.property()


    /**
     * The chart name.
     */
    @get:Input
    val chartName: Property<String> =
            project.objects.property()


    /**
     * The chart version.
     */
    @get:Input
    val chartVersion: Property<String> =
            project.objects.property()


    /**
     * The directory that contains the chart sources.
     */
    @get:InputDirectory
    val sourceDir: DirectoryProperty =
            project.objects.directoryProperty()


    /**
     * The "base" output directory. A directory with the same name as the chart
     * will be created directly below this.
     */
    @get:Internal("Represented as part of targetDir")
    val baseOutputDir: DirectoryProperty =
            project.objects.directoryProperty()
                    .convention(project.helm.outputDir)


    /**
     * The target directory, where the task will place the filtered sources.
     * This is a read-only property because the last path part must have the same name
     * as the chart.
     */
    @get:OutputDirectory
    val targetDir: Provider<Directory> =
            baseOutputDir.dir(chartName)


    /**
     * If `true` (the default), filter the requirements.yaml file by resolving dependencies on other charts
     * in the build.
     */
    @get:Input
    val resolveDependencies: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(true)


    init {
        val globalFiltering: Filtering? = project.helm.extension(HELM_FILTERING_EXTENSION_NAME)

        // we install the "filtering" block as an extension on the task so we get the convenience
        // accessors by Gradle (property, method with closure, Kotlin DSL accessor)
        createFiltering(project.objects, parent = globalFiltering)
                .apply {
                    values.putFrom("chartName", chartName)
                    values.putFrom("chartVersion", chartVersion)
                    values.putFrom("projectVersion", project.versionProvider)
                    extensions.add(Filtering::class.java, "filtering", this)
                }

        dependsOn(chartRequirementsTaskDependency())
    }


    /**
     * Settings that control filtering of the chart sources.
     */
    @get:Nested
    val filtering: Filtering
        get() = extensions.getByName("filtering") as Filtering


    @TaskAction
    fun filterSources() {
        project.copy { spec ->
            spec.from(sourceDir)
            spec.into(targetDir)
            applyFiltering(spec)
            applyDependencyResolution(spec)
        }
    }


    /**
     * Apply the [Filtering] options to a [CopySpec].
     */
    private fun applyFiltering(copySpec: CopySpec) {

        this.filtering.let { filtering ->

            if (filtering.enabled.get()) {

                // the regex to match placeholders inside the files
                val regex = Regex(
                        Regex.escape(filtering.placeholderPrefix.get()) +
                                "(.*?)" +
                                Regex.escape(filtering.placeholderSuffix.get()))

                val values = filtering.values.get()

                copySpec.filesMatching(FilteredFilePatterns) { details ->
                    details.filter { line ->
                        line.replace(regex) { matchResult ->
                            val key = matchResult.groupValues[1]
                            Objects.toString(values[key])
                        }
                    }
                }
            }
        }
    }


    /**
     * Apply dependency resolution to a [CopySpec].
     *
     * This will modify the CopySpec to filter the _requirements.yaml_ file and replace each dependency's
     * `repository` with the resolved chart directory of the dependency.
     */
    private fun applyDependencyResolution(spec: CopySpec) {
        if (resolveDependencies.get()) {

            val chartDependenciesMap = ChartDependenciesResolver.chartDependenciesMap(
                    project, configuredChartName.orNull)


            spec.filesMatching("requirements.yaml") {
                it.filter(
                        mapOf("basePath" to targetDir.get().file(it.path).asFile,
                                "chartDependenciesMap" to chartDependenciesMap),
                        RequirementsResolvingFilterReader::class.java)
            }
        }
    }


    /**
     * Returns a [TaskDependency] that represents a dependency on all the tasks that are required to build the
     * chart dependencies.
     */
    private fun chartRequirementsTaskDependency() =
            TaskDependency { task ->
                if (resolveDependencies.getOrElse(false)) {
                    configuredChartName.orNull?.let { configuredChartName ->
                        project.configurations.findByName(chartDependenciesConfigurationName(configuredChartName))
                                ?.buildDependencies?.getDependencies(task)
                    } ?: emptySet()
                } else {
                    emptySet()
                }
            }


    /**
     * A [java.io.FilterReader] that modifies the _requirements.yaml_ file by resolving the chart dependencies.
     *
     * Note: Properties of this class must be `lateinit var` because they are injected by Gradle's
     * [org.gradle.api.file.ContentFilterable.filter] method.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    internal class RequirementsResolvingFilterReader(input: Reader)
        : DelegateReader(input) {

        /**
         * The base path (= the path of the requirements.yaml file to be filtered). Paths to resolved chart
         * directories will be relative to the containing directory.
         */
        lateinit var basePath: File

        /**
         * A map of chart dependency names to the [File]s pointing to the chart directories.
         * @see ChartDependenciesResolver.chartDependenciesMap
         */
        lateinit var chartDependenciesMap: Map<String, File>


        override val delegate: Reader by lazy(LazyThreadSafetyMode.NONE) {

            val resolvedRequirementsYaml = ChartRequirementsYaml.load(`in`)
                    .withMappedDependencies { dependency ->

                        val resolvedRepository: File? = chartDependenciesMap[dependency.name]
                                ?: dependency.alias?.let { alias -> chartDependenciesMap[alias] }

                        if (resolvedRepository != null) {
                            dependency.withRepository(
                                    "file://" + resolvedRepository.relativeTo(basePath.parentFile).path)
                        } else {
                            dependency
                        }
                    }
                    .let { ChartRequirementsYaml.saveToString(it) }

            StringReader(resolvedRequirementsYaml)
        }
    }
}
