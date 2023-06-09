package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import com.citi.gradle.plugins.helm.command.helmCommandSupport
import com.citi.gradle.plugins.helm.model.ReleaseStatus
import org.unbrokendome.gradle.pluginutils.property


/**
 * Installs a chart into a remote Kubernetes cluster as a new release, or upgrades an existing release.
 *
 * This task will call `helm upgrade --install` by default, or `helm install --replace` if the release does
 * not exist or has previously failed.
 */
open class HelmInstallOrUpgrade : AbstractHelmInstallationCommandTask() {

    /**
     * If `true`, re-use the given release name, even if that name is already used.
     *
     * If this is `true`, the task will perform a `helm install --replace` command. If it is `false` (default), then
     * it will perform a `helm upgrade --install` command instead.
     */
    @get:Internal
    val replace: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    /**
     * If `true`, reset the values to the ones built into the chart when upgrading.
     *
     * Corresponds to the `--reset-values` parameter of the `helm upgrade` CLI command.
     *
     * If [replace] is set to `true`, this property will be ignored.
     */
    @get:Internal
    val resetValues: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, reuse the last release's values, and merge in any new values. If [resetValues] is specified,
     * this is ignored.

     * Corresponds to the `--reuse-values` parameter of the `helm upgrade` CLI command.
     *
     * If [replace] is set to `true`, this property will be ignored.
     */
    @get:Internal
    val reuseValues: Property<Boolean> =
        project.objects.property()


    /**
     * Limit the maximum number of revisions saved per release.
     *
     * Use `0` for no limit. If not set, the default value from Helm (currently `10`) is used.
     *
     * Corresponds to the `--history-max` parameter of the `helm upgrade` CLI command.
     *
     * If [replace] is set to `true`, this property will be ignored.
     */
    @get:Internal
    val historyMax: Property<Int> =
        project.objects.property()


    @TaskAction
    fun installOrUpgrade() {

        if (shouldUseInstallReplace()) {
            execHelm("install") {
                args(releaseName)
                args(chart)
                flag("--replace")
            }

        } else {
            execHelm("upgrade") {
                args(releaseName)
                args(chart)
                flag("--install")
                flag("--reset-values", resetValues)
                flag("--reuse-values", reuseValues)
                option("--history-max", historyMax)
            }
        }
    }


    private fun shouldUseInstallReplace(): Boolean {
        if (replace.get()) {
            return true
        }

        val release = helmCommandSupport.getRelease(releaseName)

        if (release == null) {
            logger.info("Release \"{}\" does not exist. Using 'helm upgrade --install' to install it.")
            return false
        }

        if (release.status == ReleaseStatus.FAILED) {
            logger.info("Release \"{}\" has previously failed. Using 'helm install --replace' to install it.")
            return true
        }

        return false
    }
}
