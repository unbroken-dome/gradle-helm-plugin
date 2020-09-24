package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction


/**
 * Upgrades a release on the cluster. Corresponds to the `helm upgrade` CLI command.
 */
abstract class HelmUpgrade : AbstractHelmInstallationCommandTask() {

    /**
     * If `true`, run an install if a release by this name doesn't already exist.
     */
    @get:Internal
    abstract val install: Property<Boolean>


    /**
     * If `true`, reset the values to the ones built into the chart when upgrading.
     *
     * Corresponds to the `--reset-values` CLI parameter.
     */
    @get:Internal
    abstract val resetValues: Property<Boolean>


    /**
     * If `true`, reuse the last release's values, and merge in any new values. If [resetValues] is specified,
     * this is ignored.
     *
     * Corresponds to the `--reuse-values` CLI parameter.
     */
    @get:Internal
    abstract val reuseValues: Property<Boolean>


    @TaskAction
    fun upgradeRelease() {
        execHelm("upgrade") {
            args(releaseName)
            args(chart)
            option("--version", version)
            flag("--install", install)
            flag("--reset-values", resetValues)
            flag("--reuse-values", reuseValues)
        }
    }
}
