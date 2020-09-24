package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction


/**
 * Updates the chart dependencies from the `Chart.yaml` file, and creates a `Chart.lock` file that
 * fixes the versions of chart dependencies.
 *
 * Corresponds to the `helm dependency update` CLI command.
 */
abstract class HelmUpdateDependencies : AbstractHelmDependenciesTask() {

    /**
     * If set to `true`, do not refresh the local repository cache.
     *
     * Corresponds to the `--skip-refresh` CLI option.
     */
    @get:Internal
    abstract val skipRefresh: Property<Boolean>


    init {
        inputs.file(dependencyDescriptorFile).optional()
        outputs.file(lockFile)

        onlyIf {
            // skip if the chart has no declared external dependencies
            modelDependencies.get().dependencies
                .any { it.repository != null }
        }
    }


    @TaskAction
    fun updateDependencies() {
        execHelm("dependency", "update") {
            args(chartDir)
            flag("--skip-refresh", skipRefresh)
        }
    }
}
