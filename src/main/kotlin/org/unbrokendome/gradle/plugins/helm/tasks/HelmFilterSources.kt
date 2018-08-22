package org.unbrokendome.gradle.plugins.helm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.extension
import org.unbrokendome.gradle.plugins.helm.util.property
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
            newInputDirectory()


    /**
     * The "base" output directory. A directory with the same name as the chart
     * will be created directly below this.
     */
    @get:Internal("Represented as part of targetDir")
    val baseOutputDir: DirectoryProperty =
            project.layout.directoryProperty(project.helm.outputDir)


    /**
     * The target directory, where the task will place the filtered sources.
     * This is a read-only property because the last path part must have the same name
     * as the chart.
     */
    @get:OutputDirectory
    val targetDir: Provider<Directory> =
            baseOutputDir.dir(chartName)


    init {
        val globalFiltering: Filtering? = project.helm.extension(HELM_FILTERING_EXTENSION_NAME)

        // we install the "filtering" block as an extension on the task so we get the convenience
        // accessors by Gradle (property, method with closure, Kotlin DSL accessor)
        createFiltering(project.objects, parent = globalFiltering)
                .apply {
                    values.putFrom("chartName", chartName)
                    values.putFrom("chartVersion", chartVersion)
                    extensions.add(Filtering::class.java, "filtering", this)
                }
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
}
