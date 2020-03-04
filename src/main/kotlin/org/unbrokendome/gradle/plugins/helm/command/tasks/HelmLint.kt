package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.util.GFileUtils
import org.unbrokendome.gradle.plugins.helm.command.valuesOptions
import org.unbrokendome.gradle.plugins.helm.util.ifPresent
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Runs a series of tests to verify that a chart is well-formed.
 * Corresponds to the `helm lint` CLI command.
 */
open class HelmLint : AbstractHelmCommandTask() {

    /**
     * The directory that contains the sources for the Helm chart.
     */
    @get:[InputDirectory SkipWhenEmpty]
    @Suppress("LeakingThis")
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
     */
    @get:Input
    val values: MapProperty<String, Any> =
        project.objects.mapProperty()


    /**
     * A collection of YAML files containing values to be used by the linter.
     */
    @get:InputFiles
    val valueFiles: ConfigurableFileCollection =
        project.objects.fileCollection()


    /**
     * If set, the task will create an empty marker file at this path after a successful call to `helm lint`.
     *
     * This is necessary for Gradle's up-to-date checking because `helm lint` itself doesn't output any
     * files.
     */
    @get:[OutputFile Optional]
    val outputMarkerFile: RegularFileProperty =
        project.objects.fileProperty()


    @TaskAction
    fun lint() {

        execHelm("lint") {
            flag("--strict", strict)
            valuesOptions(values, valueFiles)
            args(chartDir)
        }

        outputMarkerFile.ifPresent {
            GFileUtils.touch(it.asFile)
        }
    }
}
