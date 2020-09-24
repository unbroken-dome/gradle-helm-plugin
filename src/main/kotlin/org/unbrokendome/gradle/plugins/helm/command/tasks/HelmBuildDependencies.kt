package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.tasks.TaskAction


/**
 * Reconstructs the chart dependencies from the lock file, or mirrors the behavior of [HelmUpdateDependencies]
 * if the lock file does not exist.
 *
 * Corresponds to the `helm dependency build` CLI command.
 */
abstract class HelmBuildDependencies : AbstractHelmDependenciesTask() {

    init {
        inputs.file(lockFile)
            .withPropertyName("lockFile").optional()
        outputs.dir(subchartsDir)
            .withPropertyName("subchartsDir")

        onlyIf {
            val lockFile = project.file(this.lockFile)
            if (lockFile.exists()) {
                // regular helm dep build behavior
                false

            } else {
                // helm dep update behavior
                // skip if the chart has no declared external dependencies
                modelDependencies.get().dependencies
                    .any { it.repository != null }
            }
        }
    }


    @TaskAction
    fun buildDependencies() {
        execHelm("dependency", "build") {
            args(chartDir)
        }
    }
}
