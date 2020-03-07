package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Provides a context to execute Helm CLI commands.
 */
interface HelmExecProvider {

    /**
     * Executes a Helm CLI command.
     *
     * @param command the name of the command (e.g. `"dependency"`)
     * @param subcommand optionally, the name of the subcommand (e.g. `"update"`)
     * @param action an optional [Action] that further customizes the CLI invocation
     * @return an [ExecResult] indicating the result of the invocation
     */
    fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>? = null): ExecResult
}


/**
 * Executes a Helm CLI command.
 *
 * Extension method added for Kotlin support.
 *
 * @param command the name of the command (e.g. `"dependency"`)
 * @param subcommand optionally, the name of the subcommand (e.g. `"update"`)
 * @param action an [Action] that further customizes the CLI invocation
 * @return an [ExecResult] indicating the result of the invocation
 */
fun HelmExecProvider.execHelm(command: String, subcommand: String?, action: HelmExecSpec.() -> Unit): ExecResult =
    execHelm(command, subcommand, Action(action))


internal class HelmExecProviderSupport(
    private val project: Project,
    private val globalOptions: GlobalHelmOptions
) : HelmExecProvider {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?): ExecResult =
        project.exec { execSpec ->
            DefaultHelmExecSpec(execSpec, globalOptions, command, subcommand)
                .also { action?.execute(it) }

            globalOptions.extraArgs.ifPresent { extraArgs ->
                execSpec.args(extraArgs)
            }

            logger.info("Executing: {}", execSpec.maskedCommandLine)
        }


    private val ExecSpec.maskedCommandLine: List<String>
        get() =
            commandLine.mapIndexed { index, arg ->
                if (index > 0 && shouldMaskOptionValue(commandLine[index - 1])) "******" else arg
            }


    private fun shouldMaskOptionValue(arg: String) =
        arg.startsWith("--") && arg.contains("password")
}
