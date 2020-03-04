package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Installs a chart into the cluster. Corresponds to the `helm install` CLI command.
 */
open class HelmInstall : AbstractHelmInstallationCommandTask() {

    /**
     * If `true`, re-use the given release name, even if that name is already used.
     */
    @get:Internal
    val replace: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun install() {
        execHelm("install") {
            flag("--replace", replace)
        }
    }
}
