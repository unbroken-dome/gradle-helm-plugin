package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction


/**
 * Reconstructs the chart dependencies from the lock file, or mirrors the behavior of [HelmUpdateDependencies]
 * if the lock file does not exist.
 *
 * Corresponds to the `helm dependency build` CLI command.
 */
open class HelmBuildDependencies : AbstractHelmDependenciesTask() {

    @get:[InputFile Optional]
    final override val lockFile: Provider<RegularFile>
        get() = super.lockFile

    init {
        @Suppress("LeakingThis")
        onlyIf {
            val lockFile = project.file(this.lockFile)
            if (lockFile.exists()) {
                // regular helm dep build behavior
                true

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
