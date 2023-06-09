package com.citi.gradle.plugins.helm.testutil.exec

import com.citi.gradle.plugins.helm.testutil.startsWith
import java.io.File
import java.io.PrintWriter


/**
 * Implementation of [GradleExecMock] based on a script file that is used in place of the "real" helm executable.
 * The script forwards the arguments and environment of the process to a local server, which records them for later
 * verification.
 *
 * The reason for this roundabout way of checking the exec invocations is that it is next to impossible to inject
 * a mocked version of a service (e.g. `ExecOperations` or `ProcessOperations`) into Gradle's service registry,
 * especially when the worker API is used.
 */
interface ExecutableGradleExecMock : GradleExecMock, AutoCloseable {

    /**
     * Starts recording of invocations.
     *
     * Behaviors may still be added after this.
     */
    fun start()

    /**
     * Resets the mock, erasing all recorded invocations and clearing all behaviors.
     */
    fun reset()

    /**
     * Creates a script file that can be used instead of the "real" executable.
     */
    fun createScriptFile(scriptLocation: File)
}


class DefaultExecutableGradleExecMock : ExecutableGradleExecMock {

    private class Behavior(
        val argsPrefix: List<String>,
        val block: GradleExecBehaviorBuilder.() -> Unit
    )

    private val behaviors = mutableListOf<Behavior>()
    private val allInvocations = mutableListOf<Invocation>()
    private var registration: ExecMockServer.MockRegistration? = null


    private val callback = object : ExecMockServer.Callback {

        override fun invocation(invocation: Invocation, stdoutWriter: PrintWriter) {
            allInvocations.add(invocation)

            val behaviorBuilder = object : GradleExecBehaviorBuilder {
                override fun printsOnStdout(block: (PrintWriter) -> Unit) {
                    block(stdoutWriter)
                }
            }

            for (behavior in behaviors) {
                if (invocation.args.startsWith(behavior.argsPrefix)) {
                    behavior.block(behaviorBuilder)
                    break
                }
            }
        }
    }


    override fun start() {
        registration = execMockServer.registerMock(callback)
    }


    override fun reset() {
        behaviors.clear()
        allInvocations.clear()
    }


    override fun createScriptFile(scriptLocation: File) {
        val reg = checkNotNull(registration) { "ExecMock must be started before the script file is available." }

        scriptLocation.parentFile.mkdirs()
        scriptLocation.writeText(reg.getShellScript())
        scriptLocation.setExecutable(true)
    }


    override fun close() {
        val reg = registration
        registration = null
        reg?.unregister()
    }


    override fun forCommand(argsPrefix: List<String>): GradleExecMock =
        Prefixed(argsPrefix)


    override fun everyExec(block: GradleExecBehaviorBuilder.() -> Unit) {
        behaviors.add(Behavior(emptyList(), block))
    }


    override val invocations: List<Invocation>
        get() = allInvocations


    private inner class Prefixed(
        val prefix: List<String>
    ) : GradleExecMock {

        override fun forCommand(argsPrefix: List<String>): GradleExecMock =
            Prefixed(this.prefix + argsPrefix)


        override fun everyExec(block: GradleExecBehaviorBuilder.() -> Unit) {
            behaviors.add(Behavior(prefix, block))
        }


        override val invocations: List<Invocation>
            get() = allInvocations.filter { it.args.startsWith(prefix) }


        override fun toString(): String = buildString {
            val invocations = invocations
            append("GradleExecMock for prefix: $prefix")
            if (invocations.isEmpty()) {
                append(" (no recorded invocations)")
            } else {
                appendLine()
                appendLine("    Recorded invocations (${invocations.size}):")
                appendLine(formatInvocations(invocations))
            }
            val otherInvocations = allInvocations - invocations
            if (otherInvocations.isNotEmpty()) {
                appendLine("    Other invocations (${otherInvocations.size}):")
                appendLine(formatInvocations(otherInvocations))
            }
        }
    }


    private fun formatInvocations(invocations: List<Invocation>) =
        invocations.joinToString(separator = "\n") { "    - ${it.args}"}
}
