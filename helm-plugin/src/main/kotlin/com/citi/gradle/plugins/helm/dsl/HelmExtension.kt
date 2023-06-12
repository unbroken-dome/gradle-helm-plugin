package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.command.*
import com.citi.gradle.plugins.helm.command.internal.GlobalHelmOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmServerOptionsHolder
import org.unbrokendome.gradle.pluginutils.*
import javax.inject.Inject


/**
 * The main Helm DSL extension, accessible using the `helm { ... }` block in build scripts.
 */
interface HelmExtension : HelmExecProvider, ConfigurableGlobalHelmOptions, ConfigurableHelmServerOptions {

    /**
     * Configures the download of the Helm client executable.
     */
    val downloadClient: HelmDownloadClient


    /**
     * Configures the download of the Helm client executable.
     *
     * @param configureAction an [Action] that modifies the [HelmDownloadClient] settings
     */
    fun downloadClient(configureAction: Action<HelmDownloadClient>) {
        configureAction.execute(downloadClient)
    }


    /**
     * Base output directory for Helm charts.
     *
     * Defaults to `"${project.buildDir}/helm/charts"`.
     */
    val outputDir: DirectoryProperty


    /**
     * Base output directory for rendering (`helm template`) outputs.
     *
     * Defaults to `"${project.buildDir}/helm/render"`.
     */
    val renderOutputDir: DirectoryProperty
}


internal interface HelmExtensionInternal : HelmExtension {

    /**
     * Base temp directory where certain intermediate artifacts will be placed.
     *
     * Defaults to `"${project.buildDir}/tmp/helm"`.
     */
    val tmpDir: DirectoryProperty
}


private open class DefaultHelmExtension
@Inject constructor(
    private val project: Project,
    objects: ObjectFactory
) : HelmExtension, HelmExtensionInternal,
    ConfigurableHelmServerOptions by HelmServerOptionsHolder(objects).applyConventions(project) {

    final override val downloadClient: HelmDownloadClient =
        objects.newInstance(DefaultHelmDownloadClient::class.java, project)


    final override val executable: Property<String> =
        objects.property<String>()
            .convention(
                project.providerFromProjectProperty("helm.executable", evaluateGString = true)
            )


    final override val debug: Property<Boolean> =
        objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.debug"))


    final override val extraArgs: ListProperty<String> =
        objects.listProperty<String>().empty()


    final override val outputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.outputDir",
                    defaultPath = "\$buildDir/helm/charts", evaluateGString = true
                )
            )


    final override val renderOutputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.renderOutputDir",
                    defaultPath = "\$buildDir/helm/render", evaluateGString = true
                )
            )


    final override val tmpDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.tmpDir",
                    defaultPath = "\$buildDir/tmp/helm", evaluateGString = true
                )
            )


    final override val xdgDataHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.xdgDataHome",
                    defaultPath = "\$buildDir/helm/data", evaluateGString = true
                )
            )


    final override val xdgConfigHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.xdgConfigHome",
                    defaultPath = "\$buildDir/helm/config", evaluateGString = true
                )
            )


    final override val xdgCacheHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty(
                    "helm.xdgCacheHome",
                    defaultPath = "\$rootDir/.gradle/helm/cache", evaluateGString = true
                )
            )


    final override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?) =
        execProvider.execHelm(command, subcommand, action)


    final override fun execHelmCaptureOutput(
        command: String, subcommand: String?, action: Action<HelmExecSpec>?
    ) = execProvider.execHelmCaptureOutput(command, subcommand, action)


    private val execProvider: HelmExecProvider
        get() = HelmExecProviderSupport(project, null, this, GlobalHelmOptionsApplier)
}


private fun ConfigurableHelmServerOptions.applyConventions(project: Project) = apply {
    kubeContext.convention(
        project.providerFromProjectProperty("helm.kubeContext")
    )
    kubeConfig.convention(
        project.fileProviderFromProjectProperty("helm.kubeConfig", evaluateGString = true)
    )
    namespace.convention(
        project.providerFromProjectProperty("helm.namespace")
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
