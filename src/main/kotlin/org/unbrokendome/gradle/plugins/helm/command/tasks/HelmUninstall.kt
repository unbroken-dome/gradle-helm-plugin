package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.command.helmCommandSupport


/**
 * Uninstalls a release from the cluster. Corresponds to the `helm uninstall` CLI command.
 */
abstract class HelmUninstall : AbstractHelmServerOperationCommandTask() {

    /**
     * The name of the release to be uninstalled.
     */
    @get:Input
    abstract val releaseName: Property<String>


    /**
     * If `true`. remove all associated resources and mark the release as deleted, but retain the release history.
     *
     * Corresponds to the `--keep-history` CLI parameter.
     */
    @get:Internal
    abstract val keepHistory: Property<Boolean>


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
