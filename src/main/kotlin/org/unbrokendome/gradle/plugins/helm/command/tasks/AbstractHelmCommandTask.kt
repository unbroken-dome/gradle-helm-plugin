package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
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

    @Suppress("LeakingThis")
    private val execProviderSupport = HelmExecProviderSupport(project, this)


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


    /**
     * Modifies the [HelmExecSpec] before a Helm command is executed.
     *
     * The default implementation does nothing. Subclasses can override this if they need to modify each
     * invocation of a Helm CLI command.
     *
     * @receiver the [HelmExecSpec] to be executed
     */
    protected open fun HelmExecSpec.modifyHelmExecSpec() {}


    override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>): ExecResult =
        execProviderSupport.execHelm(command, subcommand,
            action.andThen { modifyHelmExecSpec() })


    protected fun execHelm(command: String, subcommand: String? = null, action: HelmExecSpec.() -> Unit): ExecResult =
        execHelm(command, subcommand, Action(action))
}
