package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Updates the chart dependencies from the _Chart.yaml_ file, and creates a _Chart.lock_ file that
 * fixes the versions of chart dependencies.
 *
 * Corresponds to the `helm dependency update` CLI command.
 */
open class HelmUpdateDependencies : AbstractHelmDependenciesTask() {

    /**
     * Path to the _Chart.lock_ file.
     */
    @get:OutputFile
    @Suppress("unused")
    val chartLockFile: Provider<RegularFile> =
        chartDir.file("Chart.lock")


    /**
     * If set to `true`, do not refresh the local repository cache.
     *
     * Corresponds to the `--skip-refresh` CLI option.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun updateDependencies() {
        execHelm("dependency", "update") {
            args(chartDir)
            flag("--skip-refresh", skipRefresh)
        }
    }
}
