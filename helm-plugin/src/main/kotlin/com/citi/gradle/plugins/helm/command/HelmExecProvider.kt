package com.citi.gradle.plugins.helm.command

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecSpec
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.internal.HelmOptionsApplier


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
     */
    fun execHelm(command: String, subcommand: String? = null, action: Action<HelmExecSpec>? = null)


    /**
     * Executes a Helm CLI command, and captures its output.
     *
     * @param command the name of the command
     * @param subcommand optionally, the name of the subcommand
     * @param action an [Action] that further customizes the CLI invocation
     * @return a [String] containing the captured standard output
     */
    fun execHelmCaptureOutput(
        command: String, subcommand: String? = null, action: Action<HelmExecSpec>? = null
    ): String
}


/**
 * Executes a Helm CLI command.
 *
 * Extension method added for Kotlin support.
 *
 * @param command the name of the command (e.g. `"dependency"`)
 * @param subcommand optionally, the name of the subcommand (e.g. `"update"`)
 * @param action an [Action] that further customizes the CLI invocation
 */
fun HelmExecProvider.execHelm(
    command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
) = execHelm(command, subcommand, action?.let { Action(it) })


/**
 * Executes a Helm CLI command, and captures its output.
 *
 * Extension method added for Kotlin support.
 *
 * @param command the name of the command
 * @param subcommand optionally, the name of the subcommand
 * @param action an [Action] that further customizes the CLI invocation
 * @return a [String] containing the captured standard output
 */
internal fun HelmExecProvider.execHelmCaptureOutput(
    command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
) = execHelmCaptureOutput(command, subcommand, action?.let { Action(it) })


internal class HelmExecProviderSupport(
    private val project: Project,
    private val workerExecutor: WorkerExecutor?,
    private val options: HelmOptions,
    private val optionsAppliers: Iterable<HelmOptionsApplier>,
    private val description: String? = null
) : HelmExecProvider {

    constructor(
        project: Project, workerExecutor: WorkerExecutor?, options: HelmOptions,
        optionsApplier: HelmOptionsApplier
    ) : this(project, workerExecutor, options, listOf(optionsApplier))


    private val logger = LoggerFactory.getLogger(javaClass)


    override fun execHelm(command: String, subcommand: String?, action: Action<HelmExecSpec>?) {
        if (shouldExecInWorker()) {
            execHelmInWorker(command, subcommand, action)
        } else {
            execHelmSync(command, subcommand, action)
        }
    }


    private fun shouldExecInWorker(): Boolean =
        // workerExecutor might not exist when Gradle Daemon is disabled: https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:disabling_the_daemon
        workerExecutor != null


    override fun execHelmCaptureOutput(
        command: String,
        subcommand: String?,
        action: Action<HelmExecSpec>?
    ): String {

        if (shouldExecInWorker()) {
            // Use a unique ID for this invocation, to name our stdout/stderr capture files
            val uniqueId = UUID.randomUUID().toString()
            val stdoutFile = project.buildDir.resolve("tmp/helm/$uniqueId.out")

            try {
                execHelmInWorker(command, subcommand, action, stdoutFile)
                return stdoutFile.takeIf { it.exists() }?.readText().orEmpty()

            } finally {
                stdoutFile.takeIf { it.exists() }?.delete()
            }

        } else {

            val stdout = ByteArrayOutputStream()

            execHelmSync(command, subcommand, action) {
                standardOutput = stdout
            }
            return String(stdout.toByteArray())
        }
    }


    private fun execHelmSync(
        command: String, subcommand: String?,
        action: Action<HelmExecSpec>?, withExecSpec: (ExecSpec.() -> Unit)? = null
    ) = project.exec { execSpec ->

        val helmExecSpec = DefaultHelmExecSpec(execSpec, command, subcommand)
        withExecSpec?.invoke(execSpec)
        applyOptions(helmExecSpec)
        action?.execute(helmExecSpec)

        if (logger.isInfoEnabled) {
            logger.info("Executing: {}", maskCommandLine(execSpec.commandLine))
        }
    }


    private fun execHelmInWorker(
        command: String, subcommand: String?, action: Action<HelmExecSpec>?,
        stdoutFile: File? = null
    ) {

        val workQueue = checkNotNull(workerExecutor).noIsolation()

        workQueue.submit(HelmExecWorkAction::class.java) { params ->
            params.args.add(command)
            if (subcommand != null) {
                params.args.add(subcommand)
            }

            val helmExecSpec = WorkParametersHelmExecSpec(params)

            applyOptions(helmExecSpec)
            action?.execute(helmExecSpec)

            stdoutFile?.let { params.stdoutFile.set(it) }
        }

        // If a stdoutFile was passed, we need to wait for the worker action to finish
        if (stdoutFile != null) {
            workQueue.await()
        }
    }


    private fun applyOptions(helmExecSpec: HelmExecSpec) {
        for (optionsApplier in optionsAppliers) {
            logger.debug("Calling OptionsApplier: {} with options: {}", optionsApplier, options)
            optionsApplier.apply(helmExecSpec, options)
        }
    }


    /**
     * Returns a new [HelmExecProviderSupport] that uses the given description of the operation. The description will
     * be used in logging and error reporting.
     *
     * @param description the description of the operation
     * @return a new [HelmExecProviderSupport] that uses the given description
     */
    fun withDescription(description: String): HelmExecProviderSupport =
        HelmExecProviderSupport(project, workerExecutor, options, optionsAppliers, description)


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options but a different strategy to apply them.
     *
     * @param optionsAppliers the new [HelmOptionsApplier] strategies to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun withOptionsAppliers(optionsAppliers: Iterable<HelmOptionsApplier>): HelmExecProviderSupport =
        HelmExecProviderSupport(project, workerExecutor, options, optionsAppliers, description)


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


    /**
     * Returns a new [HelmExecProviderSupport] that uses the same options and additional strategies to apply them.
     *
     * @param optionsAppliers the additional [HelmOptionsApplier] strategies to use
     * @return a new [HelmExecProviderSupport] that uses the given strategy to apply options
     */
    fun addOptionsAppliers(vararg optionsAppliers: HelmOptionsApplier) =
        withOptionsAppliers(this.optionsAppliers + optionsAppliers.toList())
}


internal fun maskCommandLine(commandLine: List<String>): List<String> =
    commandLine.mapIndexed { index, arg ->
        if (index > 0 && shouldMaskOptionValue(commandLine[index - 1])) "******" else arg
    }


private fun shouldMaskOptionValue(arg: String) =
    arg.startsWith("--") && arg.contains("password")
