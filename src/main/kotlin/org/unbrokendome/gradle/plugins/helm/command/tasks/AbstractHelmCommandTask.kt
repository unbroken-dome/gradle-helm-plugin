package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.command.DefaultHelmRunner
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmRunner
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.property


typealias HelmRunnerModifier = HelmRunner.() -> Unit


/**
 * Base class for tasks that invoke a Helm CLI command.
 */
abstract class AbstractHelmCommandTask : DefaultTask(), GlobalHelmOptions, HelmExecProvider {

    private val runnerModifiers = mutableListOf<HelmRunnerModifier>()

    init {
        group = HELM_GROUP
    }

    @get:Input
    override val executable: Property<String> =
            project.objects.property(project.helm.executable)


    @get:Internal
    override val home: DirectoryProperty =
            project.layout.directoryProperty(project.helm.home)


    @get:Console
    override val debug: Property<Boolean> =
            project.objects.property(false)


    @get:Input
    override val extraArgs: ListProperty<String> =
            project.objects.listProperty(String::class.java)
                    .apply { set(project.helm.extraArgs) }


    /**
     * Adds a modifier that will be applied to every Helm CLI invocation.
     */
    protected fun withHelmRunner(modifier: HelmRunnerModifier) {
        runnerModifiers.add(modifier)
    }


    private fun createHelmRunner(command: String, subcommand: String? = null) =
            DefaultHelmRunner(project, this, command, subcommand)
                    .apply {
                        runnerModifiers.forEach { it(this) }
                    }


    /**
     * Execute a Helm CLI command with the settings of this task.
     */
    fun execHelm(command: String, subcommand: String? = null,
                           spec: HelmRunner.() -> Unit): ExecResult =
            createHelmRunner(command, subcommand)
                    .apply(spec)
                    .run()


    override fun execHelm(command: String, subcommand: String?, spec: Action<HelmRunner>): ExecResult =
            execHelm(command, subcommand, spec::execute)
}
