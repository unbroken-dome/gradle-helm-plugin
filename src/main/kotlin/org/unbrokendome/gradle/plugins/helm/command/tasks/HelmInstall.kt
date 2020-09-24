package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction


/**
 * Installs a chart into the cluster. Corresponds to the `helm install` CLI command.
 */
abstract class HelmInstall : AbstractHelmInstallationCommandTask() {

    /**
     * If `true`, re-use the given release name, even if that name is already used.
     */
    @get:Internal
    abstract val replace: Property<Boolean>


    @TaskAction
    fun install() {
        execHelm("install") {
            args(releaseName)
            args(chart)
            option("--version", version)
            flag("--replace", replace)
        }
    }
}
