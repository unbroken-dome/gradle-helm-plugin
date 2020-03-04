package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.util.*
import java.time.Duration
import javax.inject.Inject


/**
 * The main Helm DSL extension, accessible using the `helm { ... }` block in build scripts.
 */
interface HelmExtension : HelmExecProvider, GlobalHelmOptions {

    override val executable: Property<String>

    override val debug: Property<Boolean>

    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    val kubeContext: Property<String>

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    val kubeConfig: RegularFileProperty

    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is `5m0s`.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    val remoteTimeout: Property<Duration>

    /**
     * Base output directory for Helm charts.
     *
     * Defaults to `"${project.buildDir}/helm/charts"`.
     */
    val outputDir: DirectoryProperty

    /**
     * Path to the registry config file.
     *
     * Corresponds to the `--registry-config` CLI parameter.
     * Defaults to the file `helm/registry.json` below the
     * [xdgConfigHome] directory.
     */
    override val registryConfigFile: RegularFileProperty

    /**
     * Path to the directory containing cached repository indexes.
     *
     * Corresponds to the `--repository-cache` CLI parameter. Defaults to the directory `helm/repository` below the
     * [xdgCacheHome] directory.
     */
    override val repositoryCacheDir: DirectoryProperty

    /**
     * Path to the file containing repository names and URLs.
     *
     * Corresponds to the `--repository-config` CLI parameter. Defaults to the file `helm/repositories.yaml` below
     * the [xdgConfigHome] directory.
     */
    override val repositoryConfigFile: RegularFileProperty
}


private open class DefaultHelmExtension
@Inject constructor(
    project: Project,
    objects: ObjectFactory,
    layout: ProjectLayout
) : HelmExtension {

    @Suppress("LeakingThis")
    private val execProviderSupport = HelmExecProviderSupport(project, this)


    final override val executable: Property<String> =
        objects.property<String>()
            .convention(
                project.providerFromProjectProperty(
                    "helm.executable",
                    defaultValue = "helm", evaluateGString = true
                )
            )


    final override val debug: Property<Boolean> =
        objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.debug"))


    final override val kubeContext: Property<String> =
        objects.property<String>()
            .convention(project.providerFromProjectProperty("helm.kubeContext"))


    final override val kubeConfig: RegularFileProperty =
        objects.fileProperty()
            .convention(project.fileProviderFromProjectProperty("helm.kubeConfig", evaluateGString = true))


    final override val remoteTimeout: Property<Duration> =
        objects.property<Duration>()
            .convention(project.durationProviderFromProjectProperty("helm.remoteTimeout"))


    final override val extraArgs: ListProperty<String> =
        objects.listProperty<String>().empty()


    final override val outputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.coalesceProvider(
                    project.dirProviderFromProjectProperty("helm.outputDir", evaluateGString = true),
                    layout.buildDirectory.dir("helm/charts")
                )
            )


    final override val xdgDataHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgDataHome", evaluateGString = true,
                    defaultValueProvider = project.provider { "${project.rootDir}/.gradle/helm/data" }
                )
            )


    final override val xdgConfigHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgDataHome", evaluateGString = true,
                    defaultValueProvider = project.provider { "${project.rootDir}/.gradle/helm/config" }
                )
            )


    final override val xdgCacheHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgDataHome", evaluateGString = true,
                    defaultValueProvider = project.provider { "${project.rootDir}/.gradle/helm/cache" }
                )
            )


    final override val registryConfigFile: RegularFileProperty =
        objects.fileProperty()
            .convention(xdgConfigHome.file("helm/registry.json"))


    final override val repositoryCacheDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(xdgCacheHome.dir("helm/repository"))


    final override val repositoryConfigFile: RegularFileProperty =
        objects.fileProperty()
            .convention(xdgConfigHome.file("helm/repositories.yaml"))


    final override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>): ExecResult =
        execProviderSupport.execHelm(command, subcommand, action)
}


/**
 * Creates a new [HelmExtension] object using the given project's [ObjectFactory].
 *
 * @receiver the Gradle [Project]
 * @return the created [HelmExtension] object
 */
internal fun Project.createHelmExtension(): HelmExtension =
    objects.newInstance(DefaultHelmExtension::class.java, this)
