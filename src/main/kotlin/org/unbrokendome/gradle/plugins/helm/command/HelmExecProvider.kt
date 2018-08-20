package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Action
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.command.HelmRunner


/**
 * Provides a context to execute Helm CLI commands.
 */
interface HelmExecProvider {

    /**
     * Executes a Helm CLI command.
     *
     * @param command the name of the command (e.g. `"dependency"`)
     * @param subcommand optionally, the name of the subcommand (e.g. `"update"`)
     * @param spec an [Action] that further customizes the CLI invocation
     * @return an [ExecResult] indicating the result of the invocation
     */
    fun execHelm(command: String, subcommand: String?, spec: Action<HelmRunner>): ExecResult
}
