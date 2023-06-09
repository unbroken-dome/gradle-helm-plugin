package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.pluginutils.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.toSimpleString
import java.time.Duration


/**
 * Runs the tests for a release. Corresponds to the `helm test` CLI command.
 */
open class HelmTest : AbstractHelmServerCommandTask() {

    /**
     * Name of the release to test.
     */
    @get:Input
    val releaseName: Property<String> =
        project.objects.property()


    /**
     * If `true`, dump the logs from test pods (this runs after all tests are complete, but before any cleanup).
     *
     * Corresponds to the `--logs` command line option in the Helm CLI.
     */
    @get:Console
    val showLogs: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(
                project.booleanProviderFromProjectProperty("helm.test.logs", false)
            )


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    val remoteTimeout: Property<Duration> =
        project.objects.property()


    @TaskAction
    fun test() {
        execHelm("test") {
            args(releaseName)
            flag("--logs", showLogs)
            option("--timeout", remoteTimeout.map { it.toSimpleString() })
        }
    }
}
