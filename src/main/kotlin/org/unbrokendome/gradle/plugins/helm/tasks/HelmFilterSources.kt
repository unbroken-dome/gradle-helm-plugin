package org.unbrokendome.gradle.plugins.helm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.filtering
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.AbstractYamlTransformingReader
import org.unbrokendome.gradle.plugins.helm.util.YamlPath
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.putFrom
import org.unbrokendome.gradle.plugins.helm.util.versionProvider
import java.io.Reader
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
     * The target directory, where the task will place the filtered sources.
     */
    @get:OutputDirectory
    val targetDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * If `true` (the default), the `name` and `version` entries in the Chart.yaml file will be overridden
     * with the actual values of [chartName] and [chartVersion], respectively.
     */
    @get:Input
    val overrideChartInfo: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(true)


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
        val result = project.sync { spec ->
            spec.from(sourceDir)
            spec.into(targetDir)
            spec.applyChartInfoOverrides()
            spec.applyFiltering()
        }
        didWork = result.didWork
    }


    private fun CopySpec.applyChartInfoOverrides() {
        if (overrideChartInfo.get()) {
            filesMatching("Chart.yaml") { details ->
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
    private fun CopySpec.applyFiltering() {
        if (filtering.enabled.get()) {

            // the regex to match placeholders inside the files
            val regex = Regex(
                Regex.escape(filtering.placeholderPrefix.get()) +
                        "(.*?)" +
                        Regex.escape(filtering.placeholderSuffix.get())
            )

            val values = filtering.values.get()

            filesMatching(FilteredFilePatterns) { details ->
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
}
