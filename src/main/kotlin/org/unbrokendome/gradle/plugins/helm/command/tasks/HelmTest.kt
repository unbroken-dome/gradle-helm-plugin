package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Runs the tests for a release. Corresponds to the `helm test` CLI command.
 */
class HelmTest : AbstractHelmServerCommandTask() {

    /**
     * Name of the release to test.
     */
    @get:Input
    val releaseName: Property<String> =
        project.objects.property()





    @TaskAction
    fun test() {
        execHelm("test") {
            args(releaseName)
        }
    }
}
