package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.pluginutils.fileProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty
import org.unbrokendome.gradle.pluginutils.withDefault


/**
 * Check the status for a release. Corresponds to the `helm status` CLI command.
 */
open class HelmStatus : AbstractHelmServerCommandTask() {

    /**
     * Name of the release to test the status for.
     */
    @get:Input
    val releaseName: Property<String> =
        project.objects.property()


    /**
     * If set, display the status of the named release with revision.
     *
     * Corresponds to the `--revision` CLI option.
     */
    @get:[Input Optional]
    val revision: Property<Int> =
        project.objects.property()


    /**
     * Output file for storing the status report from `helm status`.
     *
     * If not set, the status will be printed to stdout.
     */
    @get:[OutputFile Optional]
    val outputFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(
                project.fileProviderFromProjectProperty("helm.status.outputFile", evaluateGString = true)
            )


    /**
     * Fallback provider for [outputFormat] that tries to guess the correct format based on the extension
     * of the output file name.
     */
    private val formatBasedOnOutputFile: Provider<String> = outputFile.asFile.flatMap { outputFile ->
        val value: String? = when (outputFile.extension.lowercase()) {
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            else -> null
        }
        project.provider { value }
    }


    /**
     * Desired output format. Allowed values: `table`, `json`, `yaml`.
     *
     * If not set, and the [outputFile] is set, the correct output format will be guessed based on the extension of
     * the output file. For example, if [outputFile] is set to a file name that has the extension `.json`, the
     * format `json` will be used.
     *
     * Corresponds to the `--output` CLI option.
     */
    @get:[Input Optional]
    val outputFormat: Property<String> =
        project.objects.property<String>()
            .convention(
                project.providerFromProjectProperty("helm.status.outputFormat")
                    .withDefault(formatBasedOnOutputFile, project.providers)
            )


    @TaskAction
    fun status() {

        val helmExecConfig: HelmExecSpec.() -> Unit = {
            args(releaseName)
            option("--output", outputFormat)
            option("--revision", revision)
        }

        if (outputFile.isPresent) {
            val output = execHelmCaptureOutput("status", action = helmExecConfig)
            project.file(outputFile).let { outputFile ->
                outputFile.parentFile.mkdirs()
                outputFile.writeText(output)
            }

        } else {
            execHelm("status", action = helmExecConfig)
        }
    }
}
