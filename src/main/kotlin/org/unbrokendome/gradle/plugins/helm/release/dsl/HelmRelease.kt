package org.unbrokendome.gradle.plugins.helm.release.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.rules.ChartDirArtifactRule
import org.unbrokendome.gradle.plugins.helm.util.*
import java.io.File
import java.net.URI
import javax.inject.Inject


/**
 * Represents a release to be installed into or deleted from a Kubernetes cluster.
 */
interface HelmRelease : Named {

    /**
     * The release name to use for any Helm CLI command. Defaults to the [name][getName] of the release in the
     * DSL.
     */
    val releaseName: Property<String>


    /**
     * The chart to be installed, as a [ChartReference].
     *
     * Instead of setting this property directly, you should use the [from] method, which can handle a variety
     * of argument types.
     */
    val chart: Property<ChartReference>


    /**
     * Returns a reference to a chart, which can be used as a value for the [chart] property.
     *
     * @param project the path to the project that contains the chart, e.g. `":someProject"`. If `null`, refer to
     *        a named chart in the same project.
     * @param chart the name of the chart inside the Gradle project. Defaults to `"main"` if not specified.
     * @return a [ChartReference] that can be used as a value for the [chart] property
     */
    fun chart(project: String?, chart: String = "main"): ChartReference


    /**
     * Returns a reference to a chart in the same project, which can be used as a value for the [chart] property.
     *
     * @param name the name of the chart inside the Gradle project
     * @return a [ChartReference] that can be used as a value for the [chart] property
     */
    @JvmDefault
    fun chart(name: String): ChartReference =
            chart(project = null, chart = name)


    /**
     * Returns a reference to a chart that can be used as a value for the [chart] property.
     *
     * This variant is intended for Groovy DSL support, allowing us to declare a chart reference like this:
     *
     * ```
     * chart = chart(project: ':someProject', chart: 'main')
     * ```
     *
     * The following keys are supported in the `notation` map parameter:
     *   * `project`: the path to the project that contains the chart
     *   * `chart`: the name of the chart inside the Gradle project
     *
     * @param notation a [Map] containing the chart reference properties
     */
    @JvmDefault
    fun chart(notation: Map<*, *>): ChartReference =
            chart(project = notation["project"]?.toString(),
                    chart = notation["chart"]?.toString() ?: "main")


    /**
     * Sets the chart to be installed. The value can be any of the forms accepted by the Helm CLI.
     *
     * This is a convenience method that can be used instead of setting the [chart] property directly.
     *
     * The following argument types are accepted:
     *
     * - A [ChartReference], as produced by one of the [chart] methods.
     * - A `String` containing a textual chart reference, e.g. `stable/mariadb`.
     * - A [File] pointing to an unpackaged chart directory or a packaged chart file.
     * - A [Directory] pointing to an unpackaged chart directory.
     * - A [RegularFile] pointing to a packaged chart file.
     * - A [URI] pointing to a remote chart location.
     * - A [FileCollection] (for example, a Gradle [Configuration][org.gradle.api.artifacts.Configuration]) that
     *   contains a single file or directory. If the `FileCollection` has any [builtBy][FileCollection.getBuildDependencies]
     *   task dependencies, they will be honored by the `helmInstall` task associated with this release.
     * - a [Provider] of any of the above.
     *
     * @param notation an object defining the chart to be installed by this release
     */
    fun from(notation: Any)


    /**
     * Chart repository URL where to locate the requested chart.
     *
     * Use this when the [chart] property contains only a simple chart reference, without a symbolic repository name.
     */
    val repository: Property<URI>


    /**
     * The namespace to install the release into.
     *
     * Defaults to the current kubeconfig namespace.
     */
    val namespace: Property<String>


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
     */
    val version: Property<String>


    /**
     * If `true`, any action for this release will only be simulated.
     */
    val dryRun: Property<Boolean>

    /**
     * If `true`, will execute the release atomically.
     */
    val atomic: Property<Boolean>

    /**
     * If `true`, will wait until all Pods, PVCs, Services, and minimum number of Pods of a Deployment are in a ready
     * state before marking the release as successful.
     */
    val wait: Property<Boolean>


    /**
     * If `true`, the associated `helmInstall` task will replace an existing release with the same name
     * (using `helm install --replace` instead of `helm upgrade --install`).
     *
     * Defaults to `false`.
     */
    val replace: Property<Boolean>


    /**
     * If `true`, the associated `helmDelete` task will purge the release, completely removing the release from the
     * store and making its name free for later use.
     *
     * Defaults to `false`.
     */
    val purge: Property<Boolean>


    /**
     * Values to be used for the release.
     */
    val values: MapProperty<String, Any>


    /**
     * A collection of YAML files containing values for this release.
     */
    val valueFiles: ConfigurableFileCollection


    /**
     * Names of other releases that this release depends on.
     *
     * A dependency between two releases will create task dependencies such that the dependency will be installed
     * before, and deleted after, the dependent release.
     *
     * Currently such dependencies can be expressed only within the same project.
     */
    val dependsOn: SetProperty<String>


    /**
     * Express a dependency on another release.
     *
     * A dependency between two releases will create task dependencies such that the dependency will be installed
     * before, and deleted after, the dependent release.
     *
     * Currently such dependencies can be expressed only within the same project.
     */
    fun dependsOn(releaseName: String) {
        this.dependsOn.add(releaseName)
    }


    /**
     * Express a dependency on several other releases.
     */
    @JvmDefault
    fun dependsOn(vararg releaseNames: String) {
        this.dependsOn.addAll(*releaseNames)
    }
}


private open class DefaultHelmRelease
@Inject constructor(
        private val name: String,
        private val project: Project)
    : HelmRelease {

    override fun getName(): String =
            name


    override val releaseName: Property<String> =
            project.objects.property<String>()
                    .convention(name)


    override val chart: Property<ChartReference> =
            project.objects.property()


    override fun chart(project: String?, chart: String): ChartReference =
            if (project == null) {
                // Referring to a chart in the same project -> use a HelmChartReference
                HelmChartReference(this.project, chart)

            } else {
                this.project.configurations.maybeCreate("helmRelease${name.capitalizeWords()}").let { configuration ->
                    configuration.isVisible = false

                    configuration.dependencies.clear()

                    val dependency = this.project.dependencies.project(mapOf(
                            "path" to project,
                            "configuration" to ChartDirArtifactRule.getConfigurationName(chart)))
                    configuration.dependencies.add(dependency)

                    ConfigurationChartReference(this.project, configuration.name)
                }
            }


    override val repository: Property<URI> =
            project.objects.property()


    override val namespace: Property<String> =
            project.objects.property()


    override val version: Property<String> =
            project.objects.property()


    override val dryRun: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(project.booleanProviderFromProjectProperty("helm.dryRun"))

    override val atomic: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(project.booleanProviderFromProjectProperty("helm.atomic"))

    override val wait: Property<Boolean> =
            project.objects.property()


    override val replace: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(false)


    override val purge: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(false)


    override val values: MapProperty<String, Any> =
            project.objects.mapProperty()


    override val valueFiles: ConfigurableFileCollection =
            project.layout.configurableFiles()


    override val dependsOn: SetProperty<String> =
            project.objects.setProperty()


    override fun from(notation: Any) {
        if (notation is Provider<*>) {
            chart.set(notation.map(this::notationToChartReference))
        } else {
            chart.set(notationToChartReference(notation))
        }
    }


    private fun notationToChartReference(notation: Any): ChartReference =
            when (notation) {
                is ChartReference -> notation
                is FileCollection -> FileCollectionChartReference(notation)
                is HelmChart -> HelmChartReference(project, notation.name)
                else -> SimpleChartReference(notation.toString())
            }
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRelease]s.
 *
 * @param project the Gradle [Project]
 * @return the container for `HelmRelease`s
 */
internal fun helmReleaseContainer(project: Project): NamedDomainObjectContainer<HelmRelease> =
        project.container(HelmRelease::class.java) { name ->
            project.objects.newInstance(DefaultHelmRelease::class.java, name, project)
        }
