package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.emptyProperty
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Builds or updates chart dependencies. This is a combination of the `helm dependency update` and
 * `helm dependency build` CLI commands, which executes either `update` or `build` according to the following
 * logic:
 *
 * - if the _requirements.yaml_ file does not exist, deletes the _requirements.lock_ file (if present) and does not
 *   execute either of the CLI commands.
 * - if _requirements.yaml_ exists but _requirements.lock_ does not, executes `helm dependency update`.
 * - if both _requirements.yaml_ and _requirements.lock_ exist:
 *     - if _requirements.yaml_ is newer than the lock file, executes `helm dependency update`.
 *     - if the _requirements.lock_ is newer than _requirements.yaml_, executes `helm dependency build`.
 */
open class HelmBuildOrUpdateDependencies : AbstractHelmCommandTask() {

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
    @get:[InputFiles]
    @Suppress("unused")
    val requirementsYamlFile: FileCollection =
            chartDir.asFileTree.matching {
                it.include("requirements.yaml")
            }


    /**
     * A [FileCollection] containing the _requirements.lock_ file if present. This is a read-only property.
     *
     * This is modeled as a [FileCollection] so the task will not fail if the file does not exist. The collection
     * will never contain more than one file.
     */
    @get:[InputFiles]
    @Suppress("unused")
    val requirementsLockFile: FileCollection =
            chartDir.asFileTree.matching {
                it.include("requirements.lock")
            }


    /**
     * The _charts_ sub-directory; this is where sub-charts will be placed by the command (read-only).
     */
    @get:OutputDirectory
    @Suppress("unused")
    val subchartsDir: Provider<Directory> =
            chartDir.dir("charts")


    /**
     * If set to `true`, do not refresh the local repository cache when calling `helm dependency update`.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
            project.objects.emptyProperty()


    @TaskAction
    fun buildOrUpdateDependencies() {

        if (requirementsYamlFile.isEmpty) {
            // requirements.yaml does not exist. Delete requirements.lock (if present)
            chartDir.file("requirements.lock").ifPresent { it.asFile.delete() }

        } else {
            requirementsYamlFile.singleFile.let { requirementsYaml ->
                val lockFileIsPresentAndNewer = requirementsLockFile.firstOrNull()
                        ?.let { it.lastModified() > requirementsYaml.lastModified() } ?: false

                if (lockFileIsPresentAndNewer) {
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
        }
    }
}
