package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Builds or updates chart dependencies. This is a combination of the `helm dependency update` and
 * `helm dependency build` CLI commands, which executes either `update` or `build` according to the following
 * logic:
 *
 * - if _Chart.lock_ does not exist, executes `helm dependency update`.
 * - if _Chart.lock_ exists:
 *     - if _Chart.yaml_ is newer than the _Chart.lock_ file, executes `helm dependency update`.
 *     - if the _Chart.lock_ is newer than _Chart.yaml_, executes `helm dependency build`.
 */
open class HelmBuildOrUpdateDependencies : AbstractHelmDependenciesTask() {

    /**
     * A [FileCollection] containing the _requirements.lock_ file if present. This is a read-only property.
     *
     * This is modeled as a [FileCollection] so the task will not fail if the file does not exist. The collection
     * will never contain more than one file.
     */
    @get:InputFiles
    @Suppress("unused")
    val chartLockFile: FileCollection =
        chartDir.asFileTree.matching { it.include("Chart.lock") }


    /**
     * If set to `true`, do not refresh the local repository cache when calling `helm dependency update`.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun buildOrUpdateDependencies() {

        if (lockFileIsPresentAndNewer()) {
            execHelm("dependency", "build") {
                args(chartDir)
            }

        } else {
            execHelm("dependency", "update") {
                flag("--skip-refresh", skipRefresh)
                args(chartDir)
            }
        }
    }


    private fun lockFileIsPresentAndNewer(): Boolean {
        val chartYamlFile = project.file(this.chartYamlFile)
        return chartLockFile.firstOrNull()
            ?.let { it.lastModified() > chartYamlFile.lastModified() } ?: false
    }
}
