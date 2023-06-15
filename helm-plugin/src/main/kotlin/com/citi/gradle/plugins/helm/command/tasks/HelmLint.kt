package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.HelmExecProviderSupport
import com.citi.gradle.plugins.helm.command.internal.HelmValueOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmValueOptionsHolder
import java.time.Instant
import org.unbrokendome.gradle.pluginutils.ifPresent
import org.unbrokendome.gradle.pluginutils.property


/**
 * Runs a series of tests to verify that a chart is well-formed.
 * Corresponds to the `helm lint` CLI command.
 */
open class HelmLint : AbstractHelmCommandTask(), ConfigurableHelmValueOptions {

    private val valueOptions = HelmValueOptionsHolder(project.objects)

    /**
     * The directory that contains the sources for the Helm chart.
     */
    @get:[InputDirectory SkipWhenEmpty]
    val chartDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * If set to `true`, fail on warnings emitted by the linter.
     */
    @get:[Input Optional]
    val strict: Property<Boolean> =
        project.objects.property()


    /**
     * Values to be used by the linter.
     *
     * Entries in the map will be sent to the CLI using either the `--set-string` option (for strings) or the
     * `--set` option (for all other types).
     */
    @get:Input
    final override val values: MapProperty<String, Any>
        get() = valueOptions.values


    /**
     * Values read from the contents of files, to be used by the linter.
     *
     * Corresponds to the `--set-file` CLI option.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Additionally, when adding a
     * [Provider] that represents an output file of another task, the corresponding install/upgrade task will
     * automatically have a task dependency on the producing task.
     *
     * Not to be confused with [valueFiles], which contains a collection of YAML files that supply multiple values.
     */
    @get:Input
    final override val fileValues: MapProperty<String, Any>
        get() = valueOptions.fileValues


    /**
     * A collection of YAML files containing values to be used by the linter.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    @get:InputFiles
    final override val valueFiles: ConfigurableFileCollection
        get() = valueOptions.valueFiles


    /**
     * If `true`, also lint dependent charts.
     *
     * Corresponds to the `--with-subcharts` CLI option.
     */
    @get:[Input Optional]
    val withSubcharts: Property<Boolean> =
        project.objects.property()


    /**
     * If set, the task will create an empty marker file at this path after a successful call to `helm lint`.
     *
     * This is necessary for Gradle's up-to-date checking because `helm lint` itself doesn't output any
     * files.
     */
    @get:[OutputFile Optional]
    val outputMarkerFile: RegularFileProperty =
        project.objects.fileProperty()


    init {
        inputs.files(
            fileValues.keySet().map { keys ->
                keys.map { fileValues.getting(it) }
            }
        )
    }


    @TaskAction
    fun lint() {

        execHelm("lint") {
            flag("--strict", strict)
            flag("--with-subcharts", withSubcharts)
            args(chartDir)
        }

        updateOutputMarker()
    }

    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsApplier(HelmValueOptionsApplier)


    private fun updateOutputMarker() {
        outputMarkerFile.ifPresent { regularFile ->
            // file content is current time in milliseconds,
            // so we assume that lint task doesn't finish more frequent that once per millisecond
            val timeMarker = Instant.now().toEpochMilli().toString()

            regularFile.asFile.writeText(timeMarker)
        }
    }
}
