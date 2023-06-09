package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.pluginutils.property


/**
 * Updates the chart dependencies from the `Chart.yaml` file, and creates a `Chart.lock` file that
 * fixes the versions of chart dependencies.
 *
 * Corresponds to the `helm dependency update` CLI command.
 */
open class HelmUpdateDependencies : AbstractHelmDependenciesTask() {

    /**
     * If set to `true`, do not refresh the local repository cache.
     *
     * Corresponds to the `--skip-refresh` CLI option.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
        project.objects.property()


    @get:[InputFile Optional]
    final override val dependencyDescriptorFile: Provider<RegularFile>
        get() = super.dependencyDescriptorFile


    @get:[OutputFile]
    override val lockFile: Provider<RegularFile>
        get() = super.lockFile


    init {
        @Suppress("LeakingThis")
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
