package com.citi.gradle.plugins.helm.testutil.exec

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


interface ExecMockServer {

    fun registerMock(callback: Callback): MockRegistration


    interface MockRegistration {

        fun getShellScript(): String

        fun unregister()
    }


    interface Callback {

        fun invocation(invocation: Invocation, stdoutWriter: PrintWriter)
    }
}


private class DefaultExecMockServer : ExecMockServer, AutoCloseable {

    private inner class MockRegistrationImpl(
        val id: String
    ) : ExecMockServer.MockRegistration {

        override fun getShellScript(): String =
            """
            |#!/bin/bash 
            |PAYLOAD="{\
            |\"executable\":\"$0\",\
            |\"mockId\": \"$id\",\
            |\"args\":[$(for arg in "$@"; do echo "\"$(echo ${'$'}arg | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')\""; done | paste -sd ',' -)],\
            |\"env\":{$(env | awk -F '=' '{print "\"" $1 "\":\"" $2 "\""}' | paste -sd',' -)}}"
            |
            |exec curl -fqs http://localhost:${portNumber} --data-ascii "${'$'}PAYLOAD"
            """.trimMargin()


        override fun unregister() {
            registrations.remove(id)
        }
    }


    private val registrations: MutableMap<String, ExecMockServer.Callback> = ConcurrentHashMap()


    private class MockDispatcher(
        private val registrations: Map<String, ExecMockServer.Callback>
    ) : Dispatcher() {

        override fun dispatch(request: RecordedRequest): MockResponse {
            val response = MockResponse()
            try {
                val input = request.body.readString(Charsets.UTF_8)
                val body = JSONObject(input)

                val mockId = body.getString("mockId")
                val callback = registrations.getValue(mockId)

                val invocation = DefaultInvocation(
                    executable = body.getString("executable"),
                    args = body.getJSONArray("args").toList().map { it.toString() },
                    environment = body.getJSONObject("env").toMap().mapValues { (_, v) -> v.toString() }
                )

                val stdoutWriter = StringWriter()
                PrintWriter(stdoutWriter).use { stdoutPrintWriter ->
                    callback.invocation(invocation, stdoutPrintWriter)
                }

                with(response) {
                    setResponseCode(200)
                    setHeader("Content-Type", "text/plain")
                    setBody(stdoutWriter.toString())
                }
            } catch (e: Exception) {
                response.setResponseCode(500)
                response.setBody(e.toString())
            }
            return response
        }
    }


    private val server = MockWebServer().apply {
        dispatcher = MockDispatcher(registrations)
        start()
        println("Exec mock server started on port $port")
    }


    private val portNumber = server.port


    override fun registerMock(callback: ExecMockServer.Callback): ExecMockServer.MockRegistration {
        val id = UUID.randomUUID().toString()
        registrations[id] = callback
        return MockRegistrationImpl(id)
    }


    override fun close() {
        server.close()
    }
}


val execMockServer: ExecMockServer by lazy {

    DefaultExecMockServer().also { server ->
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                server.close()
            }
        )
    }
}
