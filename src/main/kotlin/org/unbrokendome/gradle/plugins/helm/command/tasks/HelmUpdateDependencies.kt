package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Updates the chart dependencies from the _requirements.yaml_ file, and creates a _requirements.lock_ file that
 * fixes the versions of chart dependencies. Corresponds to the `helm dependency update` CLI command.
 *
 * This task will be skipped with `NO_SOURCE` if the chart does not a _requirements.yaml_ file.
 */
open class HelmUpdateDependencies : AbstractHelmCommandTask() {

    /**
     * The chart directory.
     */
    @get:Internal("Represented as part of other properties")
    val chartDir: DirectoryProperty =
            project.layout.directoryProperty()


    /**
     * A [FileCollection] containing the _requirements.yaml_ file if present. This is a read-only property.
     *
     * This is modeled as a [FileCollection] so the task will not fail if the file does not exist. The collection
     * will never contain more than one file.
     */
    @get:[InputFiles SkipWhenEmpty]
    @Suppress("unused")
    val requirementsYamlFile: FileCollection =
            chartDir.asFileTree.matching {
                it.include("requirements.yaml")
            }


    /**
     * Path to the _requirements.lock_ file. This is a read-only property.
     */
    @get:OutputFile
    @Suppress("unused")
    val requirementsLockFile: Provider<RegularFile> =
            chartDir.file("requirements.lock")


    /**
     * The _charts_ sub-directory; this is where sub-charts will be placed by the command (read-only).
     */
    @get:OutputDirectory
    @Suppress("unused")
    val subchartsDir: Provider<Directory> =
            chartDir.dir("charts")


    /**
     * If set to `true`, do not refresh the local repository cache.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
            project.objects.property()


    @TaskAction
    fun updateDependencies() {
        execHelm("dependency", "update") {
            flag("--skip-refresh", skipRefresh)
            args(chartDir)
        }
    }
}
