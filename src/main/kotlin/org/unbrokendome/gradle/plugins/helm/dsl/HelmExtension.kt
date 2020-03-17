package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptionsApplier
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmExecResult
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOptions
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.dirProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.durationProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.fileProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import java.time.Duration
import javax.inject.Inject


/**
 * The main Helm DSL extension, accessible using the `helm { ... }` block in build scripts.
 */
interface HelmExtension : HelmExecProvider, GlobalHelmOptions, HelmServerOptions {

    override val executable: Property<String>

    override val debug: Property<Boolean>

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


private open class DefaultHelmExtension
@Inject constructor(
    private val project: Project,
    objects: ObjectFactory,
    layout: ProjectLayout
) : HelmExtension, HelmExtensionInternal {

    final override val executable: Property<String> =
        objects.property<String>()
            .convention(
                project.providerFromProjectProperty("helm.executable", evaluateGString = true)
                    .orElse("helm")
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


    final override val namespace: Property<String> =
        objects.property<String>()
            .convention(project.providerFromProjectProperty("helm.namespace"))


    final override val extraArgs: ListProperty<String> =
        objects.listProperty<String>().empty()


    final override val outputDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.outputDir", evaluateGString = true)
                    .orElse(layout.buildDirectory.dir("helm/charts"))
            )


    final override val tmpDir: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.tmpDir", evaluateGString = true)
                    .orElse(layout.buildDirectory.dir("tmp/helm"))
            )


    final override val xdgDataHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgDataHome", evaluateGString = true)
                    .orElse(project.layout.buildDirectory.dir("helm/data"))
            )


    final override val xdgConfigHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgConfigHome", evaluateGString = true)
                    .orElse(project.layout.buildDirectory.dir("helm/config"))
            )


    final override val xdgCacheHome: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                project.dirProviderFromProjectProperty("helm.xdgCacheHome", evaluateGString = true)
                    .orElse(project.rootDirAsDirectory.dir(".gradle/helm/cache"))
            )


    final override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?): ExecResult =
        execProviderSupport.execHelm(command, subcommand, action)


    final override fun execHelmCaptureOutput(
        command: String, subcommand: String?, action: Action<HelmExecSpec>?
    ): HelmExecResult =
        execProviderSupport.execHelmCaptureOutput(command, subcommand, action)


    private val execProviderSupport: HelmExecProviderSupport
        get() = HelmExecProviderSupport(project, this, GlobalHelmOptionsApplier)
}


/**
 * Returns the root directory of this project as a [Directory].
 *
 * @receiver the Gradle [Project]
 * @see Project.getRootDir
 */
private val Project.rootDirAsDirectory: Directory
    get() = project.rootProject.layout.projectDirectory


/**
 * Creates a new [HelmExtension] object using the given project's [ObjectFactory].
 *
 * @receiver the Gradle [Project]
 * @return the created [HelmExtension] object
 */
internal fun Project.createHelmExtension(): HelmExtension =
    objects.newInstance(DefaultHelmExtension::class.java, this)
