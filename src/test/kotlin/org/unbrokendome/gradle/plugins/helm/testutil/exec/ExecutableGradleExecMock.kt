package org.unbrokendome.gradle.plugins.helm.testutil.exec

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.unbrokendome.gradle.plugins.helm.testutil.startsWith
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter


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

    private companion object {

        fun shellScript(portNumber: Int) = """
        |#!/bin/bash 
        |PAYLOAD="{\
        |\"executable\":\"$0\",\
        |\"args\":[$(for arg in "$@"; do echo "\"$(echo ${'$'}arg | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')\""; done | paste -sd ',' -)],\
        |\"env\":{$(env | awk -F '=' '{print "\"" $1 "\":\"" $2 "\""}' | paste -sd',' -)}}"
        |
        |exec curl -fqs http://localhost:${portNumber} --data-ascii "${'$'}PAYLOAD"
        """.trimMargin()
    }

    private class Behavior(
        val argsPrefix: List<String>,
        val block: GradleExecBehaviorBuilder.() -> Unit
    )

    private val behaviors = mutableListOf<Behavior>()
    private val allInvocations = mutableListOf<Invocation>()

    private val dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val response = MockResponse()
            try {
                val input = request.body.readString(Charsets.UTF_8)
                val body = JSONObject(input)
                val invocation = DefaultInvocation(
                    executable = body.getString("executable"),
                    args = body.getJSONArray("args").toList().map { it.toString() },
                    environment = body.getJSONObject("env").toMap().mapValues { (_, v) -> v.toString() }
                )
                allInvocations.add(invocation)

                val execBehaviorBuilder = DefaultGradleExecBehaviorBuilder()

                for (behavior in behaviors) {
                    if (invocation.args.startsWith(behavior.argsPrefix)) {
                        behavior.block(execBehaviorBuilder)
                        break
                    }
                }

                with(response) {
                    setResponseCode(200)
                    setHeader("Content-Type", "text/plain")
                    setBody(execBehaviorBuilder.stdout)
                }
            } catch (e: Exception) {
                response.setResponseCode(500)
                response.setBody(e.toString())
            }
            return response
        }
    }

    private val mockServer = MockWebServer()


    override fun start() {
        mockServer.dispatcher = dispatcher
        mockServer.start()
    }


    override fun reset() {
        behaviors.clear()
        allInvocations.clear()
    }


    override fun createScriptFile(scriptLocation: File) {
        scriptLocation.parentFile.mkdirs()
        scriptLocation.writeText(shellScript(mockServer.port))
        scriptLocation.setExecutable(true)
    }


    override fun close() {
        mockServer.close()
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
                appendln()
                appendln("    Recorded invocations (${invocations.size}):")
                appendln(formatInvocations(invocations))
            }
            val otherInvocations = allInvocations - invocations
            if (otherInvocations.isNotEmpty()) {
                appendln("    Other invocations (${otherInvocations.size}):")
                appendln(formatInvocations(otherInvocations))
            }
        }
    }


    private fun formatInvocations(invocations: List<Invocation>) =
        invocations.joinToString(separator = "\n") { "    - ${it.args}"}
}



private class DefaultGradleExecBehaviorBuilder : GradleExecBehaviorBuilder {

    private val stdoutWriter = StringWriter()


    override fun printsOnStdout(block: (PrintWriter) -> Unit) {
        PrintWriter(stdoutWriter, true).use { pw ->
            block(pw)
        }
    }


    val stdout: String
        get() = stdoutWriter.toString()
}
