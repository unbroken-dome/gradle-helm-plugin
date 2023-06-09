package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import com.citi.gradle.plugins.helm.model.ChartDescriptor
import com.citi.gradle.plugins.helm.model.ChartDescriptorYaml
import com.citi.gradle.plugins.helm.model.ChartModelDependencies
import com.citi.gradle.plugins.helm.model.ChartRequirementsYaml


@Suppress("LeakingThis")
abstract class AbstractHelmDependenciesTask : AbstractHelmCommandTask() {

    /**
     * The chart directory.
     */
    @get:Internal("Represented as part of other properties")
    open val chartDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * Path to the `Chart.yaml` file of the chart (read-only).
     */
    private val chartYamlFile: Provider<RegularFile> =
        chartDir.file("Chart.yaml")


    /**
     * Path to the `requirements.yaml` file of the chart (read-only).
     */
    private val requirementsYamlFile: Provider<RegularFile> =
        chartDir.file("requirements.yaml")


    /**
     * The chart descriptor, as parsed from the `Chart.yaml` file.
     */
    @get:Internal
    internal val chartDescriptor: Provider<ChartDescriptor> =
        ChartDescriptorYaml.loading(chartYamlFile)


    /**
     * Provides the correct name of the file containing the chart's dependencies, as indicated by the
     * API version specified in the Chart.yaml file.
     */
    private val dependencyDescriptorFileName: Provider<String> =
        chartDescriptor.map { descriptor ->
            if (descriptor.apiVersion == "v1") "requirements.yaml" else "Chart.yaml"
        }


    /**
     * Provides the file containing the chart's dependencies, as indicated by the API version specified
     * in the Chart.yaml file.
     */
    @get:Internal
    internal open val dependencyDescriptorFile: Provider<RegularFile> =
        chartDir.file(dependencyDescriptorFileName)


    /**
     * Provides the correct name of the lock file, as indicated by the API version specified in the
     * Chart.yaml file.
     */
    private val lockFileName: Provider<String> =
        chartDescriptor.map { descriptor ->
            if (descriptor.apiVersion == "v1") "requirements.lock" else "Chart.lock"
        }


    /**
     * A [FileCollection] containing the `Chart.lock` _or_ the `requirements.lock` file, depending on the
     * Chart API version and only if it is present.
     */
    @get:Internal
    internal open val lockFile: Provider<RegularFile> =
        chartDir.file(lockFileName)


    @get:Internal
    internal val modelDependencies: Provider<ChartModelDependencies> =
        chartDescriptor.flatMap { descriptor ->
            if (descriptor.apiVersion == "v1") {
                ChartRequirementsYaml.loading(requirementsYamlFile)
            } else {
                chartDescriptor
            }
        }


    /**
     * The _charts_ sub-directory; this is where sub-charts will be placed by the command (read-only).
     */
    @get:OutputDirectory
    @Suppress("unused")
    val subchartsDir: Provider<Directory> =
        chartDir.dir("charts")
}
