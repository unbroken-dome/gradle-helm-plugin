package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.command.*
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.dirProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.fileProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import org.unbrokendome.gradle.plugins.helm.util.orElse
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import javax.inject.Inject


/**
 * The main Helm DSL extension, accessible using the `helm { ... }` block in build scripts.
 */
interface HelmExtension : HelmExecProvider, GlobalHelmOptions, HelmServerOptions {

    override val executable: Property<String>

    override val debug: Property<Boolean>

    override val home: DirectoryProperty

    override val host: Property<String>

    override val kubeContext: Property<String>

    override val kubeConfig: RegularFileProperty

    override val tillerNamespace: Property<String>

    /**
     * Base output directory for Helm charts.
     *
     * Defaults to `"${project.buildDir}/helm/charts"`.
     */
    val outputDir: DirectoryProperty
}


private open class DefaultHelmExtension
@Inject constructor(private val project: Project)
    : HelmExtension {

    override val executable =
            project.objects.property(
                    project.providerFromProjectProperty("helm.executable", evaluateGString = true)
                            .orElse("helm"))


    override val debug: Property<Boolean> =
            project.objects.property(
                    project.booleanProviderFromProjectProperty("helm.debug"))


    override val home: DirectoryProperty =
            project.layout.directoryProperty(
                    project.dirProviderFromProjectProperty("helm.home", evaluateGString = true))


    override val host: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.host"))


    override val kubeContext: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.kubeContext"))


    override val kubeConfig: RegularFileProperty =
            project.layout.fileProperty(
                    project.fileProviderFromProjectProperty("helm.kubeConfig", evaluateGString = true))


    override val tillerNamespace: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.tillerNamespace"))


    override val extraArgs: ListProperty<String> =
            project.objects.listProperty()


    override val outputDir: DirectoryProperty =
            project.layout.directoryProperty(
                    project.dirProviderFromProjectProperty("helm.outputDir", evaluateGString = true)
                            .orElse(project.layout.buildDirectory.dir("helm/charts")))


    override fun execHelm(command: String, subcommand: String?, spec: Action<HelmRunner>): ExecResult =
            DefaultHelmRunner(project, this, command, subcommand)
                    .also(spec::execute)
                    .run()
}


/**
 * Creates a new [HelmExtension] object using the given project's [ObjectFactory].
 *
 * @param project the Gradle [Project]
 * @return the created [HelmExtension] object
 */
fun createHelmExtension(project: Project): HelmExtension =
        project.objects.newInstance(DefaultHelmExtension::class.java, project)
