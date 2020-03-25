package org.unbrokendome.gradle.plugins.helm.testutil.exec

import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException
import java.io.OutputStream
import java.io.PrintWriter


interface GradleExecResultBuilder {

    val execSpec: ExecSpec
    val exitCode: Int


    fun writesOnStdout(block: (OutputStream) -> Unit)


    fun printsOnStdout(block: (PrintWriter) -> Unit) {
        writesOnStdout { output ->
            PrintWriter(output, true).run(block)
        }
    }


    fun printsOnStdout(s: String, newLine: Boolean = false) {
        printsOnStdout { pw ->
            pw.print(s)
            if (newLine) pw.println()
        }
    }


    fun writesOnStderr(block: (OutputStream) -> Unit)


    fun printsOnStderr(block: (PrintWriter) -> Unit) {
        writesOnStderr { output ->
            PrintWriter(output, true).run(block)
        }
    }


    fun printsOnStderr(s: String, newLine: Boolean = false) {
        printsOnStdout { pw ->
            pw.print(s)
            if (newLine) pw.println()
        }
    }


    fun returnsExitCode(exitCode: Int)


    fun buildExecResult(): ExecResult


    companion object {

        fun fromExecSpec(execSpec: ExecSpec): GradleExecResultBuilder =
            DefaultGradleExecResultBuilder(execSpec)
    }
}


private class DefaultGradleExecResultBuilder(
    override val execSpec: ExecSpec
) : GradleExecResultBuilder {

    override var exitCode: Int = 0
        private set


    override fun writesOnStdout(block: (OutputStream) -> Unit) {
        execSpec.standardOutput.run(block)
    }


    override fun writesOnStderr(block: (OutputStream) -> Unit) {
        execSpec.errorOutput.run(block)
    }


    override fun returnsExitCode(exitCode: Int) {
        this.exitCode = exitCode
    }


    override fun buildExecResult(): ExecResult =
        ExecResultImpl(exitCode)


    private class ExecResultImpl(
        private val exitCode: Int
    ) : ExecResult {

        override fun getExitValue(): Int = exitCode

        override fun assertNormalExitValue(): ExecResult = apply {
            if (exitCode != 0) {
                throw ExecException("Mock exec failed with exit code $exitCode")
            }
        }

        override fun rethrowFailure(): ExecResult = this
    }
}


