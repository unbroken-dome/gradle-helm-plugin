package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.model.ChartDescriptor
import org.unbrokendome.gradle.plugins.helm.model.ChartDescriptorYaml
import org.unbrokendome.gradle.plugins.helm.util.emptyProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Packages a chart into a versioned chart archive file. Corresponds to the `helm package` CLI command.
 *
 * The chart name and version need to be known at configuration time to determine the task outputs. If they are not
 * specified explicitly using the [chartName] and [chartVersion] properties, the task will parse the `Chart.yaml`
 * file and extract the missing information from there.
 */
open class HelmPackage : AbstractHelmCommandTask() {

    /**
     * Set the appVersion on the chart to this version.
     */
    @get:[Input Optional]
    val appVersion: Property<String> =
            project.objects.property()


    /**
     * Update dependencies from "requirements.yaml" to dir "charts/" before packaging.
     */
    @get:Input
    val updateDependencies: Property<Boolean> =
            project.objects.emptyProperty()


    /**
     * The directory that contains the sources for the Helm chart.
     */
    @get:InputDirectory
    @Suppress("LeakingThis")
    val sourceDir: DirectoryProperty =
            newInputDirectory()


    /**
     * The parsed Chart.yaml file. Implemented as a lazy [Provider] so we only parse the file when necessary.
     */
    private val chartDescriptor: Provider<ChartDescriptor> =
            sourceDir.file("Chart.yaml")
                    .let { ChartDescriptorYaml.loading(it) }


    /**
     * The name of the chart.
     *
     * If not set, the chart name will be read from the _Chart.yaml_ file in the source directory.
     */
    @get:Input
    val chartName: Property<String> =
            project.objects.property(chartDescriptor.map {
                requireNotNull(it.name) { "Chart name must either be present in Chart.yaml, or specified explicitly" }
            })


    /**
     * The version of the chart.
     *
     * If not set, the chart version will be read from the _Chart.yaml_ file in the source directory.
     */
    @get:Input
    val chartVersion: Property<String> =
            project.objects.property(chartDescriptor.map {
                requireNotNull(it.version) { "Chart version must either be present in Chart.yaml, or specified explicitly" }
            })


    /**
     * Location to write the chart archive.
     *
     * Default destination is `helm/charts/` under the project's build directory. This can also be configured
     * globally using the [HelmExtension.outputDir] property of the
     * `helm` DSL block.
     */
    @get:Internal("Represented as part of chartOutputPath")
    val destinationDir: DirectoryProperty =
            project.layout.directoryProperty(project.helm.outputDir)


    /**
     * The name of the packaged chart file.
     */
    @get:Internal("Represented as part of chartOutputPath")
    val chartFileName: Provider<String> =
            project.provider {
                "${chartName.get()}-${chartVersion.get()}.tgz"
            }


    /**
     * The full path of the packaged chart file (read-only property).
     */
    @get:OutputFile
    val chartOutputPath: Provider<RegularFile> =
            destinationDir.file(chartFileName)


    /**
     * Indicates whether the chart should also be saved to the local repository after packaging.
     *
     * Corresponds to the `--save` command line parameter. Defaults to `false`.
     */
    @get:Input
    val saveToLocalRepo: Property<Boolean> =
            project.objects.property(false)


    init {
        @Suppress("LeakingThis")
        inputs.dir(home)
    }


    @TaskAction
    fun helmPackage() {

        // Make sure the destination directory exists, otherwise helm package will fail.
        this.destinationDir.get().asFile.mkdirs()

        execHelm("package") {
            option("--app-version", appVersion)
            flag("--dependency-update", updateDependencies)
            option("--destination", destinationDir)
            flag("--save", saveToLocalRepo, true)
            args(sourceDir)
        }
    }
}
