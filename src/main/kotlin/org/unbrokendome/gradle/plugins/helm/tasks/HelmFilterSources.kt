package org.unbrokendome.gradle.plugins.helm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.ResolvedChartDependency
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.chartDependenciesConfigurationName
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.helmDependencies
import org.unbrokendome.gradle.plugins.helm.dsl.filtering
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.model.ChartDescriptorYaml
import org.unbrokendome.gradle.plugins.helm.model.ChartModelDependencies
import org.unbrokendome.gradle.plugins.helm.model.ChartRequirementsYaml
import org.unbrokendome.gradle.plugins.helm.model.map
import org.unbrokendome.gradle.plugins.helm.util.AbstractYamlTransformingReader
import org.unbrokendome.gradle.plugins.helm.util.YamlPath
import org.unbrokendome.gradle.plugins.helm.util.ifPresent
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.putFrom
import org.unbrokendome.gradle.plugins.helm.util.versionProvider
import java.io.File
import java.io.Reader
import java.util.BitSet
import java.util.Objects


private val FilteredFilePatterns = listOf("Chart.yaml", "values.yaml", "requirements.yaml")


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


    /**
     * If `true` (the default), the `name` and `version` entries in the Chart.yaml file will be overridden
     * with the actual values of [chartName] and [chartVersion], respectively.
     */
    @get:Input
    val overrideChartInfo: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(true)


    init {
        dependsOn(chartRequirementsTaskDependency())
    }


    /**
     * Settings that control filtering of the chart sources.
     */
    @get:Nested
    val filtering: Filtering = project.objects.createFiltering(parent = project.helm.filtering)
        .apply {
            values.putFrom("chartName", chartName)
            values.putFrom("chartVersion", chartVersion)
            values.putFrom("projectVersion", project.versionProvider)
        }


    /**
     * Configures filtering for this task.
     */
    @Suppress("unused")
    fun filtering(configureAction: Action<Filtering>) {
        configureAction.execute(filtering)
    }


    @TaskAction
    fun filterSources() {
        project.copy { spec ->
            spec.from(sourceDir)
            spec.into(targetDir)
            applyChartInfoOverrides(spec)
            applyFiltering(spec)
            applyDependencyResolution(spec)
        }
    }


    private fun applyChartInfoOverrides(copySpec: CopySpec) {
        if (overrideChartInfo.get()) {
            copySpec.filesMatching("Chart.yaml") { details ->
                details.filter(
                    mapOf("overrides" to mapOf(
                        "name" to chartName.get(),
                        "version" to chartVersion.get()
                    )),
                    YamlOverrideFilterReader::class.java
                )
            }
        }
    }


    /**
     * Apply the [Filtering] options to a [CopySpec].
     */
    private fun applyFiltering(copySpec: CopySpec) {
        if (filtering.enabled.get()) {

            // the regex to match placeholders inside the files
            val regex = Regex(
                Regex.escape(filtering.placeholderPrefix.get()) +
                        "(.*?)" +
                        Regex.escape(filtering.placeholderSuffix.get())
            )

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


    /**
     * Apply dependency resolution to a [CopySpec].
     *
     * This will modify the CopySpec to filter the _Chart.yaml_ and/or _requirements.yaml_ file and replace
     * each dependency's `repository` and `version` with the resolved dependency.
     *
     * @param spec the [CopySpec]
     */
    private fun applyDependencyResolution(spec: CopySpec) {
        if (resolveDependencies.get()) {

            configuredChartName.ifPresent { configuredChartName ->

                project.configurations.findByName(chartDependenciesConfigurationName(configuredChartName))
                    ?.let { configuration ->
                        val chartDependencyResolver = { name: String ->
                            configuration.helmDependencies[name]?.resolve(project, configuration)
                        }
                        spec.filesMatching("Chart.yaml") {
                            it.applyDependencyResolution(chartDependencyResolver) {
                                ChartDescriptorYaml.load(
                                    project.file(sourceDir.file("Chart.yaml"))
                                )
                            }
                        }
                        // Also allow dependencies from a requirements.yaml file for apiVersion v1 compatibility
                        spec.filesMatching("requirements.yaml") {
                            it.applyDependencyResolution(chartDependencyResolver) {
                                ChartRequirementsYaml.load(
                                    project.file(sourceDir.file("requirements.yaml"))
                                )
                            }
                        }
                    }
            }
        }
    }


    /**
     * Applies a filter to the [FileCopyDetails] receiver that resolves dependencies from the chart model.
     *
     * @receiver a [FileCopyDetails] that configures the current copy-filter operation
     * @param chartDependencyResolver a function that resolves a chart dependency for a chart name or alias
     * @param chartModelDependenciesSupplier a function that lazily reads the [ChartModelDependencies]
     */
    private fun FileCopyDetails.applyDependencyResolution(
        chartDependencyResolver: (String) -> ResolvedChartDependency?,
        chartModelDependenciesSupplier: () -> ChartModelDependencies
    ) = filter(
            mapOf(
                "overrideDependencies" to chartModelDependenciesSupplier().resolve(
                    basePath = targetDir.get().file(path).asFile,
                    chartDependencyResolver = chartDependencyResolver
                )
            ),
            YamlDependenciesOverrideFilterReader::class.java
        )


    /**
     * Resolves dependencies from the chart model (either from a Chart.yaml or a requirements.yaml file)
     *
     * @param basePath the path to the model file declaring the dependency; repository URLs will be rewritten as
     *        relative paths based on the directory it is in
     * @param chartDependencyResolver a function that resolves a chart dependency for a chart name or alias
     * @return a new [ChartModelDependencies] object containing the resolved dependencies
     */
    private fun ChartModelDependencies.resolve(
        basePath: File,
        chartDependencyResolver: (String) -> ResolvedChartDependency?
    ): ChartModelDependencies =
        map { modelDependency ->
            val resolved = chartDependencyResolver(modelDependency.name)
                ?: modelDependency.alias?.let(chartDependencyResolver)
            if (resolved != null) {
                modelDependency.withRepositoryAndVersion(
                    repository = "file://" + resolved.file.relativeTo(basePath.parentFile).path,
                    version = resolved.version
                )
            } else {
                modelDependency
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
                        ?.let { chartDependenciesConfiguration ->

                            val externalDependencies: Set<Task> =
                                chartDependenciesConfiguration.buildDependencies.getDependencies(task)

                            // Unfortunately just calling Configuration.getBuildDependencies() isn't enough,
                            // because that doesn't consider artifacts in the same project.
                            val internalDependencies: Set<Task> =
                                chartDependenciesConfiguration.allArtifacts.buildDependencies.getDependencies(task)

                            externalDependencies + internalDependencies
                        }

                } ?: emptySet()
            } else {
                emptySet()
            }
        }


    /**
     * A [java.io.FilterReader] that modifies a given YAML file by overriding specific values.
     *
     * Overridden values must be provided by setting the `overrides` property.
     *
     * Any values that are already present in the source and have corresponding entries in [overrides] will be
     * overridden in-place with the new values. Entries in [overrides] that do not appear in the original source
     * will be appended at the end.
     *
     * Note: Properties of this class must be `lateinit var` because they are injected by Gradle's
     * [org.gradle.api.file.ContentFilterable.filter] method.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    internal class YamlOverrideFilterReader(input: Reader) : AbstractYamlTransformingReader(input) {

        var overrides: Map<String, Any> = emptyMap()

        override fun transformScalar(path: YamlPath, value: String): String? =
            overrides[path.toString()]?.toString()
    }


    /**
     * A [java.io.FilterReader] that modifies the _requirements.yaml_ file by resolving the chart dependencies.
     *
     * Note: Properties of this class must be `lateinit var` because they are injected by Gradle's
     * [org.gradle.api.file.ContentFilterable.filter] method.
     */
    internal class YamlDependenciesOverrideFilterReader(input: Reader) : AbstractYamlTransformingReader(input) {

        /**
         * The dependencies to override.
         */
        private lateinit var _overrideDependencies: ChartModelDependencies

        private lateinit var repositoryWritten: BitSet
        private lateinit var versionWritten: BitSet


        var overrideDependencies: ChartModelDependencies
            get() = _overrideDependencies
            set(value) {
                _overrideDependencies = value
                repositoryWritten = BitSet(value.dependencies.size)
                versionWritten = BitSet(value.dependencies.size)
            }


        override fun transformScalar(path: YamlPath, value: String): String? {
            val pathElements = path.elements
            if (pathElements.size == 3 && (pathElements[0] as? YamlPath.Element.MappingKey)?.name == "dependencies") {
                val index = (pathElements[1] as? YamlPath.Element.SequenceIndex)?.index
                val property = (pathElements[2] as? YamlPath.Element.MappingKey)?.name

                if (index != null && property != null) {
                    val modelDependency = overrideDependencies.dependencies[index]
                    return when (property) {
                        "repository" -> {
                            modelDependency.repository
                                .also { repositoryWritten.set(index) }
                        }
                        "version" -> {
                            modelDependency.version
                                .also { versionWritten.set(index) }
                        }
                        else -> null
                    }
                }
            }

            return null
        }


        override fun addToMapping(path: YamlPath): Map<String, String> {
            val pathElements = path.elements
            if (pathElements.size == 2 && (pathElements[0] as? YamlPath.Element.MappingKey)?.name == "dependencies") {
                val index = (pathElements[1] as? YamlPath.Element.SequenceIndex)?.index

                if (index != null) {
                    // Add the "repository" and "version" properties for this dependency if they have not been written
                    val modelDependency = overrideDependencies.dependencies[index]
                    return listOfNotNull(
                        modelDependency.repository.takeIf { !repositoryWritten[index] }
                            ?.let { "repository" to it },
                        modelDependency.version.takeIf { !versionWritten[index] }
                            ?.let { "version" to it }
                    ).toMap()
                }
            }

            return emptyMap()
        }
    }
}
