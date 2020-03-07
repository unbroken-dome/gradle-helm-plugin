package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.andThen
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Base class for tasks that invoke a Helm CLI command.
 */
abstract class AbstractHelmCommandTask : DefaultTask(), GlobalHelmOptions, HelmExecProvider {

    init {
        group = HELM_GROUP
    }


    @get:Input
    override val executable: Property<String> =
        project.objects.property<String>()
            .convention(project.helm.executable)


    @get:Console
    override val debug: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    @get:Input
    override val extraArgs: ListProperty<String> =
        project.objects.listProperty<String>()
            .apply { addAll(project.helm.extraArgs) }


    @get:Internal
    final override val registryConfigFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(project.helm.registryConfigFile)


    @get:Internal
    final override val repositoryCacheFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(project.helm.repositoryCacheFile)


    @get:Internal
    final override val repositoryConfigFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(project.helm.repositoryConfigFile)


    /**
     * Modifies the [HelmExecSpec] before a Helm command is executed.
     *
     * The default implementation does nothing. Subclasses can override this if they need to modify each
     * invocation of a Helm CLI command.
     *
     * @param exec the [HelmExecSpec] to be executed
     */
    protected open fun modifyHelmExecSpec(exec: HelmExecSpec) {}


    override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?): ExecResult {
        val modifyAction = Action<HelmExecSpec> { modifyHelmExecSpec(it) }
        return execProviderSupport.execHelm(
            command, subcommand,
            action?.andThen(modifyAction) ?: modifyAction
        )
    }


    protected fun execHelm(command: String, subcommand: String? = null, action: HelmExecSpec.() -> Unit): ExecResult =
        execHelm(command, subcommand, Action(action))


    private val execProviderSupport: HelmExecProviderSupport
        get() = HelmExecProviderSupport(project, this)
}
