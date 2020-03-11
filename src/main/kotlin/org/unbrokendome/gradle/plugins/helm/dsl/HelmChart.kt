package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Buildable
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_MAIN_CHART_NAME
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage
import org.unbrokendome.gradle.plugins.helm.model.ChartDescriptor
import org.unbrokendome.gradle.plugins.helm.model.ChartDescriptorYaml
import org.unbrokendome.gradle.plugins.helm.model.ChartModelDependencies
import org.unbrokendome.gradle.plugins.helm.model.ChartRequirementsYaml
import org.unbrokendome.gradle.plugins.helm.rules.packageTaskName
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.versionProvider
import javax.inject.Inject


/**
 * Represents a Helm chart built by this project.
 */
interface HelmChart : Named, Buildable {

    /**
     * The chart name.
     *
     * By default, the chart will have the same name as in the Gradle DSL (except for the "main" chart which
     * will have the same name as the project by default).
     */
    val chartName: Property<String>


    /**
     * The chart version.
     *
     * By default, the chart will have the same version as the Gradle project.
     */
    val chartVersion: Property<String>


    /**
     * The directory that contains the chart sources.
     */
    val sourceDir: DirectoryProperty


    /**
     * The base output directory. When the chart is built, a subdirectory with the same name as the chart
     * will be created below this. (That subdirectory can be queried using [outputDir].)
     *
     * By default, this is the same as the [HelmExtension.outputDir] configured on the global `helm` extension.
     *
     * @see outputDir
     */
    val baseOutputDir: DirectoryProperty


    /**
     * The directory where the (exploded) chart files will be written.
     *
     * This is read-only (a [Provider], not a [Property]) because Helm demands that the chart directory has
     * the same name as the chart. To change the base output directory, set the [baseOutputDir] property to
     * a different value.
     *
     * @see baseOutputDir
     */
    @JvmDefault
    val outputDir: Provider<Directory>
        get() = baseOutputDir.flatMap { it.dir(chartName) }


    /**
     * The name of the packaged chart file.
     */
    @JvmDefault
    val packageFileName: Provider<String>
        get() = HelmPackage.packagedChartFileName(chartName, chartVersion)


    /**
     * The location of the packaged chart file.
     *
     * The file will be placed inside the [baseOutputDir] and have a name according to the pattern
     * `<chart>-<version>.tgz`.
     */
    @JvmDefault
    val packageOutputFile: Provider<RegularFile>
        get() = baseOutputDir.flatMap { it.file(packageFileName) }
}


internal interface HelmChartInternal : HelmChart {

    /**
     * The directory where the filtered sources of the chart will be placed.
     */
    val filteredSourcesDir: Provider<Directory>

    /**
     * The chart descriptor, as parsed from the Chart.yaml file.
     */
    val chartDescriptor: Provider<ChartDescriptor>

    /**
     * The dependencies, as declared in either the Chart.yaml file (for API version v2) or the
     * requirements.yaml file (for API version v1).
     *
     * If the API version is v1 and no requirements.yaml file exists, the provider will produce
     * an empty [ChartModelDependencies].
     */
    val modelDependencies: Provider<ChartModelDependencies>
}


private open class DefaultHelmChart
@Inject constructor(
    private val name: String,
    defaultVersion: Provider<String>,
    baseOutputDir: Provider<Directory>,
    filteredSourcesBaseDir: Provider<Directory>,
    objects: ObjectFactory
) : HelmChart, HelmChartInternal {

    final override fun getName(): String =
        name


    final override val chartName: Property<String> =
        objects.property<String>()
            .convention(name)


    final override val chartVersion: Property<String> =
        objects.property<String>()
            .convention(defaultVersion)


    final override val sourceDir: DirectoryProperty =
        objects.directoryProperty()


    final override val baseOutputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(baseOutputDir)


    final override fun getBuildDependencies(): TaskDependency =
        TaskDependency { task ->
            if (task != null) {
                setOf(
                    task.project.tasks.getByName(packageTaskName)
                )
            } else {
                emptySet()
            }
        }


    final override val filteredSourcesDir: Provider<Directory> =
        filteredSourcesBaseDir.flatMap { it.dir(chartName) }


    final override val chartDescriptor: Provider<ChartDescriptor> =
        ChartDescriptorYaml.loading(sourceDir.file("Chart.yaml"))


    final override val modelDependencies: Provider<ChartModelDependencies> =
        chartDescriptor.flatMap { descriptor ->
            if (descriptor.apiVersion == "v1") {
                ChartRequirementsYaml.loading(sourceDir.file("requirements.yaml"))
            } else {
                chartDescriptor
            }
        }
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmChart]s.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmChart`s
 */
internal fun Project.helmChartContainer(
    baseOutputDir: Provider<Directory>,
    filteredSourcesBaseDir: Provider<Directory>
): NamedDomainObjectContainer<HelmChart> =
    container(HelmChart::class.java) { name ->
        objects.newInstance<HelmChart>(
            DefaultHelmChart::class.java, name,
            this.versionProvider, baseOutputDir, filteredSourcesBaseDir
        ).also { chart ->
            // The "main" chart should be named like the project by default
            if (name == HELM_MAIN_CHART_NAME) {
                chart.chartName.convention(this@helmChartContainer.name)
            }
        }
    }
