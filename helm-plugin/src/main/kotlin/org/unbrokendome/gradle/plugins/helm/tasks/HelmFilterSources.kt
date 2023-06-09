package com.citi.gradle.plugins.helm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.putFrom
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.dsl.Filtering
import com.citi.gradle.plugins.helm.dsl.createFiltering
import com.citi.gradle.plugins.helm.util.filterYaml
import org.unbrokendome.gradle.pluginutils.io.expand
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.versionProvider


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
    val filtering: Filtering = project.objects.createFiltering()
        .apply {
            values.putFrom("chartName", chartName)
            values.putFrom("chartVersion", chartVersion)
            values.putFrom("projectVersion", project.versionProvider)
        }


    init {
        val fileValues = filtering.fileValues
        inputs.files(
            fileValues.keySet().map { keys ->
                keys.map { fileValues.getting(it) }
            }
        )
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
            spec.applyFiltering()
            spec.applyChartInfoOverrides()
        }
        didWork = result.didWork
    }


    private fun CopySpec.applyChartInfoOverrides() {
        if (overrideChartInfo.get()) {
            filesMatching("Chart.yaml") { details ->
                details.filterYaml(
                    "name" to chartName.get(),
                    "version" to chartVersion.get()
                )
            }
        }
    }


    /**
     * Apply the [Filtering] options to a [CopySpec].
     */
    private fun CopySpec.applyFiltering() {
        if (filtering.enabled.get()) {

            val values = filtering.values.get()
            val valuesFromFiles = filtering.fileValues.get()
                .mapValues { (_, value) ->
                    project.files(value).singleFile.readText()
                }

            val filePatterns = filtering.filePatterns.get()
                .takeIf { it.isNotEmpty() }
                ?: listOf("*")

            filesMatching(filePatterns) { details ->
                details.expand(valuesFromFiles + values, true)
            }
        }
    }
}
