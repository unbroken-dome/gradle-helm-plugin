package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.emptyProperty
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


    /**
     * If `true`, delete test pods upon completion.
     */
    @get:Internal
    val cleanup: Property<Boolean> =
            project.objects.emptyProperty()


    @TaskAction
    fun test() {
        execHelm("test") {
            flag("--cleanup", cleanup)
            args(releaseName)
        }
    }
}
