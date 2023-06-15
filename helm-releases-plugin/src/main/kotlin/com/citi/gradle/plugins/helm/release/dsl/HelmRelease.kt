package com.citi.gradle.plugins.helm.release.dsl

import org.gradle.api.*
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableHelmInstallFromRepositoryOptions
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.internal.*
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.rules.ChartDirArtifactRule
import org.unbrokendome.gradle.pluginutils.*
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


/**
 * Contains the configurable properties of a Helm release; these can be modified on the core release object
 * as well as a target-specific variant.
 */
interface HelmReleaseProperties : Named, ConfigurableHelmInstallFromRepositoryOptions, ConfigurableHelmValueOptions {

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
     * @return a [ChartReference] that can be used as a value for the [chart] property
     */
    fun chart(notation: Map<*, *>): ChartReference =
        chart(
            project = notation["project"]?.toString(),
            chart = notation["chart"]?.toString() ?: "main"
        )


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
     * If `true`, the associated `helmInstall` task will replace an existing release with the same name
     * (using `helm install --replace` instead of `helm upgrade --install`).
     *
     * Defaults to `false`.
     */
    val replace: Property<Boolean>


    /**
     * Limit the maximum number of revisions saved per release.
     *
     * Use `0` for no limit. If not set, the default value from Helm (currently `10`) is used.
     *
     * Corresponds to the `--history-max` parameter of the `helm upgrade` CLI command.
     *
     * If [replace] is set to `true`, this property will be ignored.
     */
    val historyMax: Property<Int>


    /**
     * If `true`, the associated `helmUninstall` task will retain the release history
     * (using the `--keep-history` flag).
     *
     * Defaults to `false`.
     */
    val keepHistoryOnUninstall: Property<Boolean>


    /**
     * Names of other releases that this release depends on.
     *
     * A dependency between two releases will create task dependencies such that the dependency will be installed
     * before, and uninstalled after, the dependent release.
     *
     * Currently such dependencies can be expressed only within the same project.
     *
     * Deprecated as of 1.2.0; build scripts should indicate such dependencies using release tags instead, and
     * specify the preferred installation/uninstallation order using [mustInstallAfter] and [mustUninstallAfter].
     */
    @Deprecated(message = "use release tags instead")
    val dependsOn: SetProperty<String>


    /**
     * Express a dependency on another release.
     *
     * A dependency between two releases will create task dependencies such that the dependency will be installed
     * before, and uninstalled after, the dependent release.
     *
     * Currently such dependencies can be expressed only within the same project.
     *
     * Deprecated as of 1.2.0; build scripts should indicate such dependencies using release tags instead, and
     * specify the preferred installation/uninstallation order using [mustInstallAfter] and [mustUninstallAfter].
     */
    @Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
    @Deprecated(message = "use release tags instead")
    fun dependsOn(releaseName: String) {
        this.dependsOn.add(releaseName)
    }


    /**
     * Express a dependency on several other releases.
     *
     * Deprecated as of 1.2.0; build scripts should indicate such dependencies using release tags instead, and
     * specify the preferred installation/uninstallation order using [mustInstallAfter] and [mustUninstallAfter].
     */
    @Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
    @Deprecated(message = "use release tags instead")
    fun dependsOn(vararg releaseNames: String) {
        this.dependsOn.addAll(*releaseNames)
    }


    /**
     * A set of additional dependencies for the task that installs this release.
     * May contain any of the notations supported by [Task.dependsOn].
     *
     * @see [Task.dependsOn]
     */
    val installDependsOn: MutableSet<Any>


    /**
     * Declare additional dependencies for the task that installs this release.
     *
     * @param paths Additional installation dependencies.
     *              May contain any of the notations supported by [Task.dependsOn].
     */
    fun installDependsOn(paths: Iterable<Any>) {
        this.installDependsOn.addAll(paths)
    }


    /**
     * Declare additional dependencies for the task that installs this release.
     *
     * @param paths Additional installation dependencies.
     *              May contain any of the notations supported by [Task.dependsOn].
     */
    fun installDependsOn(vararg paths: Any) {
        installDependsOn(paths.toList())
    }


    /**
     * A set of release names that this release must be installed after, if installation of both releases is
     * requested by the current build.
     *
     * @see Task.mustRunAfter
     */
    val mustInstallAfter: MutableSet<String>


    /**
     * Specifies that this release must be installed after all the supplied releases.
     *
     * @param releaseNames names of releases that this release must be installed after
     * @see Task.mustRunAfter
     */
    fun mustInstallAfter(vararg releaseNames: String) {
        mustInstallAfter.addAll(releaseNames.toList())
    }


    /**
     * A set of release names that this release must be uninstalled after, if uninstallation of both releases is
     * requested by the current build.
     *
     * @see Task.mustRunAfter
     */
    val mustUninstallAfter: MutableSet<String>


    /**
     * Specifies that this release must be uninstalled after all the supplied releases.
     *
     * @param releaseNames names of releases that this release must be uninstalled after
     * @see Task.mustRunAfter
     */
    fun mustUninstallAfter(vararg releaseNames: String) {
        mustUninstallAfter.addAll(releaseNames.toList())
    }


    /**
     * Access testing configuration options for this release.
     */
    val test: ConfigurableHelmReleaseTestOptions


    /**
     * Configure testing options for this release.
     *
     * @param configureAction an [Action] to configure testing options for this release
     */
    fun test(configureAction: Action<ConfigurableHelmReleaseTestOptions>) {
        configureAction.execute(this.test)
    }
}


/**
 * Represents a release to be installed into or uninstalled from a Kubernetes cluster.
 *
 * Provides methods for target-specific configuration of the release.
 */
interface HelmRelease : Named, HelmReleaseProperties, ConfigurableHelmInstallFromRepositoryOptions {

    /**
     * Tags for this release.
     *
     * Tags can be used to filter the set of releases to be installed or uninstalled. Releases that do not have any
     * tags will always be installed.
     */
    val tags: MutableSet<String>


    /**
     * Declare tags for this release.
     *
     * Tags can be used to filter the set of releases to be installed or uninstalled. Releases that do not have any
     * tags will always be installed.
     *
     * @param tags the tags to use for this release
     */
    fun tags(tags: Iterable<String>) {
        this.tags.addAll(tags)
    }


    /**
     * Declare tags for this release.
     *
     * Tags can be used to filter the set of releases to be installed or uninstalled. Releases that do not have any
     * tags will always be installed.
     *
     * @param tags the tags to use for this release
     */
    fun tags(vararg tags: String) {
        tags(tags.toList())
    }


    /**
     * A list of directories that contain target-specific values files for the release.
     *
     * From each directory in the list, in the order of appearance, the following files are used if they exist:
     * * A file named `values.yaml`
     * * A file named `values-<target>.yaml`, where `<target>` is the currently active release target.
     */
    val valuesDirs: ListProperty<File>


    /**
     * Adds directories containing values files to the release.
     *
     * @param directories the directories to add. Each entry is evaluated as per [Project.file].
     */
    fun valuesDirs(directories: Iterable<Any>)


    /**
     * Adds directories containing values files to the release.
     *
     * @param directories the directories to add. Each entry is evaluated as per [Project.file].
     */
    fun valuesDirs(vararg directories: Any) {
        valuesDirs(directories.toList())
    }


    /**
     * Adds a directory containing values files to the release.
     *
     * @param directory the directory to add. It is evaluated as per [Project.file].
     */
    fun valuesDir(directory: Any) {
        valuesDirs(listOf(directory))
    }


    /**
     * Adds a target-specific configuration action for the given release targets.
     *
     * Target names can be prefixed with an exclamation mark (e.g. `!target`) to apply the configuration for
     * any _but_ the given target.
     *
     * @param targets the names of release targets
     * @param action the target-specific configuration to apply
     */
    fun forTargets(targets: Iterable<String>, action: Action<TargetSpecific>)


    /**
     * Adds a target-specific configuration action for the given release targets.
     *
     * Target names can be prefixed with an exclamation mark (e.g. `!target`) to apply the configuration for
     * any _but_ the given target.
     *
     * @param targets the names of release targets
     * @param action the target-specific configuration to apply
     */
    fun forTargets(vararg targets: String, action: Action<TargetSpecific>) =
        forTargets(targets.toList(), action)


    /**
     * Adds a target-specific configuration action for the given release target.
     *
     * The target name can be prefixed with an exclamation mark (e.g. `!target`) to apply the configuration for
     * any _but_ the given target.
     *
     * @param target the name of the release target
     * @param action the target-specific configuration to apply
     */
    fun forTarget(target: String, action: Action<TargetSpecific>) =
        forTargets(listOf(target), action)


    /**
     * Adds a target-specific configuration action for any target.
     *
     * Use this method to specify a custom configuration block that depends on the [HelmReleaseTarget], which is
     * available via the [TargetSpecific.target] property in the action parameter.
     *
     * @param action the target-specific configuration to apply
     */
    fun forAnyTarget(action: Action<TargetSpecific>) =
        forTarget("", action)


    /**
     * Used to configure a [HelmRelease] for a specific release target.
     */
    interface TargetSpecific : HelmReleaseProperties {

        /**
         * The [HelmReleaseTarget] for which target-specific configuration is being applied.
         */
        val target: HelmReleaseTarget
    }
}


internal interface HelmReleaseInternal {

    /**
     * Returns a [HelmReleaseProperties] with target-specific configuration applied
     * when evaluated.
     *
     * @param target the [HelmReleaseTarget] for which to apply target-specific configuration
     * @return a [HelmReleaseProperties] with target-specific configuration applied
     */
    fun resolveForTarget(target: HelmReleaseTarget): HelmReleaseProperties
}


private abstract class AbstractHelmRelease(
    private val name: String,
    protected val project: Project
) : Named, HelmReleaseProperties,
    ConfigurableHelmInstallFromRepositoryOptions by HelmInstallFromRepositoryOptionsHolder(project.objects),
    ConfigurableHelmValueOptions by HelmValueOptionsHolder(project.objects) {

    override fun getName(): String =
        name


    final override val releaseName: Property<String> =
        project.objects.property<String>()
            .convention(name)


    final override val chart: Property<ChartReference> =
        project.objects.property()


    final override fun from(notation: Any) {
        if (notation is Provider<*>) {
            chart.set(notation.map { notationToChartReference(it) })
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


    final override fun chart(project: String?, chart: String): ChartReference =
        if (project == null) {
            // Referring to a chart in the same project -> use a HelmChartReference
            HelmChartReference(this.project, chart)

        } else {
            this.project.configurations.maybeCreate("helmRelease${name.capitalizeWords()}").let { configuration ->
                configuration.isVisible = false

                configuration.dependencies.clear()

                val dependency = this.project.dependencies.project(
                    mapOf(
                        "path" to project,
                        "configuration" to ChartDirArtifactRule.getConfigurationName(chart)
                    )
                )
                configuration.dependencies.add(dependency)

                ConfigurationChartReference(this.project, configuration.name)
            }
        }


    final override val replace: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    final override val historyMax: Property<Int> =
        project.objects.property()


    final override val keepHistoryOnUninstall: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    @Suppress("OverridingDeprecatedMember")
    final override val dependsOn: SetProperty<String> =
        project.objects.setProperty()


    final override val installDependsOn: MutableSet<Any> = mutableSetOf()


    final override val mustInstallAfter: MutableSet<String> = mutableSetOf()


    final override val mustUninstallAfter: MutableSet<String> = mutableSetOf()


    final override val test =
        DefaultHelmReleaseTestOptions(project.objects)
}


private open class DefaultHelmRelease
@Inject constructor(
    name: String,
    project: Project
) : AbstractHelmRelease(name, project), HelmRelease, HelmReleaseInternal {

    private val targetSpecificActions = mutableMapOf<String, Action<HelmRelease.TargetSpecific>>()
    private val targetSpecificCache: MutableMap<String, HelmReleaseProperties> = ConcurrentHashMap()


    override val tags = mutableSetOf<String>()


    override val valuesDirs: ListProperty<File> =
        project.objects.listProperty()


    override fun valuesDirs(directories: Iterable<Any>) {
        for (directory in directories) {
            when (directory) {
                is Directory -> valuesDirs.add(directory.asFile)
                is File -> valuesDirs.add(directory)
                is String -> valuesDirs.add(project.file(directory))
                else -> {
                    val provider = project.layout.dir(project.provider { project.file(directory) }).asFile()
                    valuesDirs.add(provider)
                }
            }
        }
    }


    private fun getValuesFilesForAnyTarget(): FileCollection =
        getValuesFilesWithFileName("values.yaml")


    private fun getValuesFilesForTarget(target: String): FileCollection =
        getValuesFilesWithFileName("values-$target.yaml")


    private fun getValuesFilesWithFileName(fileName: String): FileCollection {
        val provider = valuesDirs.map { dirs ->
            dirs.asSequence()
                .map { it.resolve(fileName) }
                .filter { it.exists() }
                .asIterable()
                .let { project.files(it) }
        }
        return project.files(provider)
    }


    override fun forTargets(targets: Iterable<String>, action: Action<HelmRelease.TargetSpecific>) {
        for (target in targets) {
            targetSpecificActions.merge(target, action) { action1, action2 -> action1.andThen(action2) }
        }
    }


    override fun resolveForTarget(target: HelmReleaseTarget): HelmReleaseProperties =
        targetSpecificCache.computeIfAbsent(target.name) {
            doResolveForTarget(target)
        }


    private fun doResolveForTarget(target: HelmReleaseTarget): HelmReleaseProperties {

        val logger = LoggerFactory.getLogger(DefaultHelmRelease::class.java)

        logger.info("Constructing target-specific release \"{}\" for target \"{}\"", this.name, target.name)

        return TargetSpecific(name, project, target).also { targetSpecific ->

            // Call setFrom(HelmInstallFromRepositoryOptions) to assign all the options properties that only exist
            // on HelmRelease, but not on HelmReleaseTarget
            targetSpecific.setFrom(this)
            // Call setFrom(HelmInstallationOptions) to assign the options properties that exist on both HelmRelease
            // and HelmReleaseTarget; using properties from the release and falling back to release target
            targetSpecific.setFrom(this.withDefaults(target, project.providers))

            // Assign all the other properties that don't map to an option
            targetSpecific.releaseName.set(this.releaseName)
            targetSpecific.chart.set(this.chart)
            targetSpecific.replace.set(this.replace)
            targetSpecific.historyMax.set(this.historyMax)
            targetSpecific.keepHistoryOnUninstall.set(this.keepHistoryOnUninstall)
            @Suppress("DEPRECATION")
            targetSpecific.dependsOn.addAll(this.dependsOn)
            targetSpecific.installDependsOn.addAll(this.installDependsOn)
            targetSpecific.mustInstallAfter.addAll(this.mustInstallAfter)
            targetSpecific.mustUninstallAfter.addAll(this.mustUninstallAfter)

            with(targetSpecific.test) {
                setFrom(test.withDefaults(target.test, project.providers))
                enabled.convention(true)
                timeout.convention(remoteTimeout)
            }

            // Merge in the values from all sources
            logger.debug("Merging values from release target")
            targetSpecific.mergeValues(target)
            logger.debug("Merging values from release")
            targetSpecific.mergeValues(this)
            targetSpecific.valueFiles.from(getValuesFilesForAnyTarget())
            targetSpecific.valueFiles.from(getValuesFilesForTarget(target.name))

            // Apply all matching target-specific actions
            val action = (
                    // action for any target
                    listOfNotNull(targetSpecificActions[""]) +
                            // negative-match actions for other targets
                            targetSpecificActions.filterKeys { it.startsWith("!") && it != "!${target.name}" }.values +
                            // positive-match action for the given target
                            listOfNotNull(targetSpecificActions[target.name])
                    ).combine()
            if (action != null) {
                logger.debug("Applying target-specific configuration actions")
                action.execute(targetSpecific)
            } else {
                logger.debug("No target-specific configuration actions specified")
            }
        }
    }


    private class TargetSpecific(
        name: String,
        project: Project,
        override val target: HelmReleaseTarget
    ) : AbstractHelmRelease(name, project), HelmRelease.TargetSpecific
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRelease]s.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmRelease`s
 */
internal fun Project.helmReleaseContainer(): NamedDomainObjectContainer<HelmRelease> =
    container(HelmRelease::class.java) { name ->
        objects.newInstance(DefaultHelmRelease::class.java, name, this)
    }
