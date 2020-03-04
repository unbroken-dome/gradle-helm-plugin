package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction


/**
 * Builds the chart dependencies from the _Chart.lock_ or _Chart.yaml_ file. Corresponds to the
 * `helm dependency build` CLI command.
 */
open class HelmBuildDependencies : AbstractHelmDependenciesTask() {

    /**
     * A [FileCollection] containing the _Chart.lock_ file if present. This is a read-only property.
     *
     * This is modeled as a [FileCollection] so the task will not fail if the file does not exist. The collection
     * will never contain more than one file.
     */
    @get:[InputFiles SkipWhenEmpty]
    @Suppress("unused")
    val chartLockFile: FileCollection =
        chartDir.asFileTree.matching { it.include("Chart.lock") }


    @TaskAction
    fun buildDependencies() {
        execHelm("dependency", "build") {
            args(chartDir)
        }
    }
}
