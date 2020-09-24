package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmValueOptionsApplier
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Runs a series of tests to verify that a chart is well-formed.
 * Corresponds to the `helm lint` CLI command.
 */
@Suppress("LeakingThis")
abstract class HelmLint : AbstractHelmCommandTask(), ConfigurableHelmValueOptions {

    /**
     * The directory that contains the sources for the Helm chart.
     */
    @get:[InputDirectory SkipWhenEmpty]
    abstract val chartDir: DirectoryProperty


    /**
     * If set to `true`, fail on warnings emitted by the linter.
     */
    @get:[Input Optional]
    abstract val strict: Property<Boolean>


    /**
     * If set, the task will create an empty marker file at this path after a successful call to `helm lint`.
     *
     * This is necessary for Gradle's up-to-date checking because `helm lint` itself doesn't output any
     * files.
     */
    @get:[OutputFile Optional]
    abstract val outputMarkerFile: RegularFileProperty


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
