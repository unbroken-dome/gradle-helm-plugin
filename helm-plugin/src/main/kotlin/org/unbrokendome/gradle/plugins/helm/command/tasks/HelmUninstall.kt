package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.command.helmCommandSupport
import org.unbrokendome.gradle.pluginutils.property


/**
 * Uninstalls a release from the cluster. Corresponds to the `helm uninstall` CLI command.
 */
open class HelmUninstall : AbstractHelmServerOperationCommandTask() {

    /**
     * The name of the release to be uninstalled.
     */
    @get:Input
    val releaseName: Property<String> =
        project.objects.property()


    /**
     * If `true`. remove all associated resources and mark the release as deleted, but retain the release history.
     *
     * Corresponds to the `--keep-history` CLI parameter.
     */
    @get:Internal
    val keepHistory: Property<Boolean> =
        project.objects.property()


    init {
        outputs.upToDateWhen {
            !doesReleaseExist()
        }
    }


    @TaskAction
    fun uninstallRelease() {

        execHelm("uninstall") {
            args(releaseName)
            flag("--dry-run", dryRun)
            flag("--keep-history", keepHistory)
        }
    }


    private fun doesReleaseExist(): Boolean {
        val release = helmCommandSupport.getRelease(releaseName)
        if (release == null) {
            logger.info("Release \"{}\" does not exist. Skipping uninstall.")
            return false
        }
        return true
    }
}
