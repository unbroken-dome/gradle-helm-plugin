package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.*
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.HELM_MAIN_CHART_NAME
import com.citi.gradle.plugins.helm.command.tasks.HelmPackage
import com.citi.gradle.plugins.helm.model.ChartDescriptor
import com.citi.gradle.plugins.helm.model.ChartDescriptorYaml
import com.citi.gradle.plugins.helm.model.ChartModelDependencies
import com.citi.gradle.plugins.helm.model.ChartRequirementsYaml
import com.citi.gradle.plugins.helm.rules.DefaultRenderingRule
import com.citi.gradle.plugins.helm.rules.packageTaskName
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.versionProvider
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
    val outputDir: Provider<Directory>
        get() = baseOutputDir.flatMap { it.dir(chartName) }


    /**
     * The name of the packaged chart file.
     */
    val packageFileName: Provider<String>
        get() = HelmPackage.packagedChartFileName(chartName, chartVersion)


    /**
     * The location of the packaged chart file.
     *
     * The file will be placed inside the [baseOutputDir] and have a name according to the pattern
     * `<chart>-<version>.tgz`.
     */
    val packageFile: Provider<RegularFile>


    /**
     * The location of the packaged chart file.
     *
     * The file will be placed inside the [baseOutputDir] and have a name according to the pattern
     * `<chart>-<version>.tgz`.
     *
     * @deprecated use [packageFile] instead
     */
    @Deprecated(message = "use packageFile", replaceWith = ReplaceWith("packageFile"))
    val packageOutputFile: Provider<RegularFile>
        get() = packageFile


    /**
     * A [CopySpec] that allows copying additional files into the chart.
     */
    val extraFiles: CopySpec


    /**
     * Configures a [CopySpec] that allows copying additional files into the chart.
     *
     * @param action an [Action] to configure on the [extraFiles] `CopySpec`
     */
    fun extraFiles(action: Action<CopySpec>) {
        action.execute(extraFiles)
    }


    /**
     * The base output directory for renderings. When a rendering is executed using
     * `helm template`, a subdirectory with the name of the rendering will be created
     * below this. (That subdirectory can be queried using [HelmRendering.outputDir].)
     *
     * By default, this is a subdirectory with the name of the chart, under the global base directory
     * configured using [HelmExtension.renderOutputDir] on the global `helm` extension.
     */
    val renderBaseOutputDir: DirectoryProperty


    /**
     * The renderings for this chart.
     *
     * Each rendering allows specifying a different set of values and other options to pass
     * to `helm template`. If no renderings are added to this container, the plugin will
     * add a single rendering named "default" with no values.
     */
    val renderings: NamedDomainObjectContainer<HelmRendering>


    /**
     * Configures the renderings for this chart.
     *
     * Each rendering allows specifying a different set of values and other options to pass
     * to `helm template`. If no renderings are added to this container, the plugin will
     * add a single rendering named "default" with no values.
     *
     * @param action an [Action] to configure the renderings for this chart
     */
    fun renderings(action: Action<NamedDomainObjectContainer<HelmRendering>>) {
        action.execute(renderings)
    }


    /**
     * If `true`, override the `name` and `version` fields in the Chart.yaml file with the
     * values of [chartName] and [chartVersion], respectively. This step is performed after
     * any filtering (as configured by the `filtering` block).
     *
     * Defaults to `true`.
     */
    val overrideChartInfo: Property<Boolean>
}


internal interface HelmChartInternal : HelmChart {

    /**
     * The directory where the filtered sources of the chart will be placed.
     */
    val filteredSourcesDir: Provider<Directory>

    /**
     * The directory where the resolved dependencies (subcharts) of the chart will be placed.
     */
    val dependenciesDir: Provider<Directory>

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
    project: Project,
    baseOutputDir: Provider<Directory>,
    globalRenderBaseOutputDir: Provider<Directory>,
    filteredSourcesBaseDir: Provider<Directory>,
    dependenciesBaseDir: Provider<Directory>,
    objects: ObjectFactory
) : HelmChart, HelmChartInternal {

    private val tasks = project.tasks


    final override fun getName(): String =
        name


    final override val chartName: Property<String> =
        objects.property<String>()
            .convention(name)


    final override val chartVersion: Property<String> =
        objects.property<String>()
            .convention(project.versionProvider)


    final override val sourceDir: DirectoryProperty =
        objects.directoryProperty()


    final override val baseOutputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(baseOutputDir)


    final override val packageFile: Provider<RegularFile>
        get() = tasks.named(packageTaskName, HelmPackage::class.java).flatMap { it.packageFile }


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


    override val dependenciesDir: Provider<Directory> =
        dependenciesBaseDir.flatMap { it.dir(chartName) }


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


    override val extraFiles: CopySpec = project.copySpec()


    final override val renderBaseOutputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                globalRenderBaseOutputDir.map { it.dir(name) }
            )


    final override val renderings: NamedDomainObjectContainer<HelmRendering> =
        objects.domainObjectContainer(HelmRendering::class.java) { name ->
            objects.createHelmRendering(name, this.name)
                .apply {
                    outputDir.convention(renderBaseOutputDir.dir(name))
                }
        }.also { container ->
            container.addRule(DefaultRenderingRule(container))
        }


    final override val overrideChartInfo: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(true)
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmChart]s.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmChart`s
 */
internal fun Project.helmChartContainer(
    baseOutputDir: Provider<Directory>,
    globalRenderBaseOutputDir: Provider<Directory>,
    filteredSourcesBaseDir: Provider<Directory>,
    dependenciesBaseDir: Provider<Directory>
): NamedDomainObjectContainer<HelmChart> =
    container(HelmChart::class.java) { name ->
        objects.newInstance<HelmChart>(
            DefaultHelmChart::class.java, name,
            this, baseOutputDir, globalRenderBaseOutputDir, filteredSourcesBaseDir,
            dependenciesBaseDir
        ).also { chart ->
            // The "main" chart should be named like the project by default
            if (name == HELM_MAIN_CHART_NAME) {
                chart.chartName.convention(this@helmChartContainer.name)
            }
        }
    }
