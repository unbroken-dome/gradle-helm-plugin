package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream


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
    fun execHelm(command: String, subcommand: String? = null, action: Action<HelmExecSpec>? = null): ExecResult


    /**
     * Executes a Helm CLI command, and captures its output.
     *
     * @param command the name of the command
     * @param subcommand optionally, the name of the subcommand
     * @param action an [Action] that further customizes the CLI invocation
     * @return a [HelmExecResult] that describes the result of the invocation and contains the captured output
     */
    fun execHelmCaptureOutput(
        command: String, subcommand: String? = null, action: Action<HelmExecSpec>? = null
    ): HelmExecResult
}


/**
 * Provides information about the result of a Helm CLI invocation.
 */
data class HelmExecResult(
    /** The Gradle [ExecResult]. */
    val execResult: ExecResult,
    /** The captured standard output. */
    val stdout: String,
    /** The captured standard error output. */
    val stderr: String
)


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
fun HelmExecProvider.execHelm(
    command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
): ExecResult =
    execHelm(command, subcommand, action?.let { Action(it) })


/**
 * Executes a Helm CLI command, and captures its output.
 *
 * Extension method added for Kotlin support.
 *
 * @param command the name of the command
 * @param subcommand optionally, the name of the subcommand
 * @param action an [Action] that further customizes the CLI invocation
 * @return a [HelmExecResult] that describes the result of the invocation and contains the captured output
 */
internal fun HelmExecProvider.execHelmCaptureOutput(
    command: String, subcommand: String? = null, action: HelmExecSpec.() -> Unit
) = execHelmCaptureOutput(command, subcommand, Action(action))


internal class HelmExecProviderSupport(
    private val project: Project,
    private val options: HelmOptions,
    private val optionsAppliers: Iterable<HelmOptionsApplier>,
    private val description: String? = null
) : HelmExecProvider {

    constructor(project: Project, options: HelmOptions, optionsApplier: HelmOptionsApplier)
    : this(project, options, listOf(optionsApplier))


    private val logger = LoggerFactory.getLogger(javaClass)


    override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?): ExecResult =
        project.exec { execSpec ->

            val helmExecSpec = DefaultHelmExecSpec(execSpec, command, subcommand)

            val optionsAppliersCalled = mutableSetOf<HelmOptionsApplier>()

            fun applyAll(optionsApplier: HelmOptionsApplier) {
                if (optionsAppliersCalled.add(optionsApplier)) {
                    optionsApplier.implies.forEach { applyAll(it) }
                    logger.debug("Calling OptionsApplier: {} with options: {}", optionsApplier, options)
                    optionsApplier.apply(helmExecSpec, options)
                }
            }

            optionsAppliers.forEach(::applyAll)

            action?.execute(helmExecSpec)

            logger.info("Executing: {}", execSpec.maskedCommandLine)
        }


    override fun execHelmCaptureOutput(
        command: String,
        subcommand: String?,
        action: Action<HelmExecSpec>?
    ): HelmExecResult {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val execResult = execHelm(command, subcommand) {
            action?.execute(this)
            withExecSpec {
                standardOutput = stdout
                errorOutput = stderr
                isIgnoreExitValue = true
            }
        }
        if (execResult.exitValue != 0) {
            throw HelmExecException(
                command, subcommand, execResult, String(stderr.toByteArray()),
                description?.let { "Error trying to $it" }
            )
        }
        return HelmExecResult(execResult, String(stdout.toByteArray()), String(stderr.toByteArray()))
    }


    /**
     * Returns a new [HelmExecProviderSupport] that uses the given description of the operation. The description will
     * be used in logging and error reporting.
     *
     * @param description the description of the operation
     * @return a new [HelmExecProviderSupport] that uses the given description
     */
    fun withDescription(description: String): HelmExecProviderSupport =
        HelmExecProviderSupport(project, options, optionsAppliers, description)


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options but a different strategy to apply them.
     *
     * @param optionsAppliers the new [HelmOptionsApplier] strategies to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun withOptionsAppliers(optionsAppliers: Iterable<HelmOptionsApplier>): HelmExecProviderSupport =
        HelmExecProviderSupport(project, options, optionsAppliers, description)


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options but a different strategy to apply them.
     *
     * @param optionsApplier the new [HelmOptionsApplier] strategy to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun withOptionsApplier(optionsApplier: HelmOptionsApplier) =
        withOptionsAppliers(listOf(optionsApplier))


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options but a different strategy to apply them.
     *
     * @param optionsAppliers the new [HelmOptionsApplier] strategies to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun withOptionsAppliers(vararg optionsAppliers: HelmOptionsApplier) =
        withOptionsAppliers(optionsAppliers.toList())


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options and an additional strategy to apply them.
     *
     * @param optionsApplier the additional [HelmOptionsApplier] strategy to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun addOptionsApplier(optionsApplier: HelmOptionsApplier) =
        withOptionsAppliers(this.optionsAppliers + optionsApplier)


    private val ExecSpec.maskedCommandLine: List<String>
        get() =
            commandLine.mapIndexed { index, arg ->
                if (index > 0 && shouldMaskOptionValue(commandLine[index - 1])) "******" else arg
            }


    private fun shouldMaskOptionValue(arg: String) =
        arg.startsWith("--") && arg.contains("password")
}
