package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.process.ExecResult


class HelmExecException(
    val command: String,
    val subcommand: String?,
    val execResult: ExecResult,
    val stderr: String?,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(
    message?.plus(": ").orEmpty() +
            "Invocation of 'helm $command${subcommand?.let { " $it" }}' failed with exit code ${execResult.exitValue}" +
            stderr?.let { "\nError output:\n$it" },
    cause
)
