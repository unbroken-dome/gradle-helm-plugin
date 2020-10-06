package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.gradle.util.GradleVersion
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmValueOptionsApplier
import org.unbrokendome.gradle.plugins.helm.util.GRADLE_VERSION_5_3
import org.unbrokendome.gradle.plugins.helm.util.ifPresent
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Runs a series of tests to verify that a chart is well-formed.
 * Corresponds to the `helm lint` CLI command.
 */
open class HelmLint : AbstractHelmCommandTask(), ConfigurableHelmValueOptions {

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
    final override val values: MapProperty<String, Any> =
        project.objects.mapProperty()


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
    final override val fileValues: MapProperty<String, Any> =
        project.objects.mapProperty()


    /**
     * A collection of YAML files containing values to be used by the linter.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    @get:InputFiles
    final override val valueFiles: ConfigurableFileCollection =
        if (GradleVersion.current() >= GRADLE_VERSION_5_3) {
            project.objects.fileCollection()
        } else {
            @Suppress("DEPRECATION")
            project.layout.configurableFiles()
        }


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
            args(chartDir)
        }

        outputMarkerFile.ifPresent {
            GFileUtils.touch(it.asFile)
        }
    }


    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsApplier(HelmValueOptionsApplier)
}
