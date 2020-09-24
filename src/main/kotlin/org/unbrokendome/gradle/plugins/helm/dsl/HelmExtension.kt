package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableGlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmServerOptions
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptionsApplier
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.dirProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.fileProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import javax.inject.Inject


/**
 * The main Helm DSL extension, accessible using the `helm { ... }` block in build scripts.
 */
interface HelmExtension : HelmExecProvider, ConfigurableGlobalHelmOptions, ConfigurableHelmServerOptions {

    /**
     * Configures the download of the Helm client executable.
     */
    @get:Nested
    val downloadClient: HelmDownloadClient


    /**
     * Configures the download of the Helm client executable.
     *
     * @param configureAction an [Action] that modifies the [HelmDownloadClient] settings
     */
    @JvmDefault
    fun downloadClient(configureAction: Action<HelmDownloadClient>) {
        configureAction.execute(downloadClient)
    }


    /**
     * Base output directory for Helm charts.
     *
     * Defaults to `"${project.buildDir}/helm/charts"`.
     */
    val outputDir: DirectoryProperty
}


internal interface HelmExtensionInternal : HelmExtension {

    /**
     * Base temp directory where certain intermediate artifacts will be placed.
     *
     * Defaults to `"${project.buildDir}/tmp/helm"`.
     */
    val tmpDir: DirectoryProperty
}


private abstract class DefaultHelmExtension
@Inject constructor(
    private val project: Project,
    objects: ObjectFactory
) : HelmExtension, HelmExtensionInternal {

    final override val downloadClient: HelmDownloadClient =
        objects.newInstance(DefaultHelmDownloadClient::class.java, project)


    final override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?) =
        execProvider.execHelm(command, subcommand, action)


    final override fun execHelmCaptureOutput(
        command: String, subcommand: String?, action: Action<HelmExecSpec>?
    ) = execProvider.execHelmCaptureOutput(command, subcommand, action)


    private val execProvider: HelmExecProvider
        get() = HelmExecProviderSupport(project, null, this, GlobalHelmOptionsApplier)
}


private fun HelmExtensionInternal.applyConventions(project: Project) = apply {
    kubeContext.convention(
        project.providerFromProjectProperty("helm.kubeContext")
    )
    kubeConfig.convention(
        project.fileProviderFromProjectProperty("helm.kubeConfig", evaluateGString = true)
    )
    namespace.convention(
        project.providerFromProjectProperty("helm.namespace")
    )
    debug.convention(
        project.booleanProviderFromProjectProperty("helm.debug")
    )
    executable.convention(
        project.providerFromProjectProperty("helm.executable", evaluateGString = true)
    )
    extraArgs.empty()
    outputDir.convention(
        project.dirProviderFromProjectProperty(
            "helm.outputDir", defaultPath = "\$buildDir/helm/charts", evaluateGString = true
        )
    )
    tmpDir.convention(
        project.dirProviderFromProjectProperty(
            "helm.tmpDir", defaultPath = "\$buildDir/tmp/helm", evaluateGString = true
        )
    )
    xdgDataHome.convention(
        project.dirProviderFromProjectProperty(
            "helm.xdgDataHome", defaultPath = "\$buildDir/helm/data", evaluateGString = true
        )
    )
    xdgConfigHome.convention(
        project.dirProviderFromProjectProperty(
            "helm.xdgConfigHome", defaultPath = "\$buildDir/helm/config", evaluateGString = true
        )
    )
    xdgCacheHome.convention(
        project.dirProviderFromProjectProperty(
            "helm.xdgCacheHome", defaultPath = "\$rootDir/.gradle/helm/cache", evaluateGString = true
        )
    )
}


/**
 * Creates a new [HelmExtension] object using the given project's [ObjectFactory].
 *
 * @receiver the Gradle [Project]
 * @return the created [HelmExtension] object
 */
internal fun Project.createHelmExtension(): HelmExtension =
    objects.newInstance(DefaultHelmExtension::class.java, this)
        .applyConventions(this)
