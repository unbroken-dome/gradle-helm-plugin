package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.toHelmString
import java.time.Duration


/**
 * Runs the tests for a release. Corresponds to the `helm test` CLI command.
 */
@Suppress("LeakingThis")
abstract class HelmTest : AbstractHelmServerCommandTask() {

    /**
     * Name of the release to test.
     */
    @get:Input
    abstract val releaseName: Property<String>


    /**
     * If `true`, dump the logs from test pods (this runs after all tests are complete, but before any cleanup).
     *
     * Corresponds to the `--logs` command line option in the Helm CLI.
     */
    @get:Console
    abstract val showLogs: Property<Boolean>


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    abstract val remoteTimeout: Property<Duration>


    init {
        showLogs.convention(
            project.booleanProviderFromProjectProperty("helm.test.logs", false)
        )
    }


    @TaskAction
    fun test() {
        execHelm("test") {
            args(releaseName)
            flag("--logs", showLogs)
            option("--timeout", remoteTimeout.map { it.toHelmString() })
        }
    }
}
