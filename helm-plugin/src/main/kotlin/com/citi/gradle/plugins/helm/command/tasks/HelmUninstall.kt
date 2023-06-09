package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import com.citi.gradle.plugins.helm.command.helmCommandSupport
import org.unbrokendome.gradle.pluginutils.property


/**
 * Uninstalls a release from the cluster. Corresponds to the `helm uninstall` CLI command.
 */
@Suppress("LeakingThis")
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


    /**
     * If `true`, will wait until all the resources are deleted before returning.
     * It will wait for as long as [remoteTimeout].
     *
     * Corresponds to the `--wait` CLI parameter.
     */
    @get:Internal
    val wait: Property<Boolean> =
        project.objects.property()


    init {
        onlyIf {
            if (!doesReleaseExist()) {
                logger.info(
                    "Release \"{}\" does not exist. Skipping uninstall.",
                    releaseName.orNull
                )
                false
            } else true
        }
    }


    @TaskAction
    fun uninstallRelease() {

        execHelm("uninstall") {
            args(releaseName)
            flag("--dry-run", dryRun)
            flag("--keep-history", keepHistory)
            flag("--wait", wait)
        }
    }


    private fun doesReleaseExist(): Boolean =
        helmCommandSupport.getRelease(releaseName) != null
}
