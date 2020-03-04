package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Deletes a release from the cluster. Corresponds to the `helm delete` CLI command.
 */
open class HelmDelete : AbstractHelmServerCommandTask() {

    /**
     * The name of the release to be deleted.
     */
    @get:Input
    val releaseName: Property<String> =
        project.objects.property()


    /**
     * If `true`, simulate a delete.
     */
    @get:Internal
    val dryRun: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`. remove all associated resources and mark the release as deleted, but retain the release history.
     *
     * Corresponds to the `--keep-history` CLI parameter.
     */
    @get:Internal
    val keepHistory: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun deleteRelease() {
        execHelm("uninstall") {
            args(releaseName)
            flag("--dry-run", dryRun)
            flag("--keep-history", keepHistory)
        }
    }
}
