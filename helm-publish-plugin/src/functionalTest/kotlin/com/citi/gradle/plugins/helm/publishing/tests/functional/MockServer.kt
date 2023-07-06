package com.citi.gradle.plugins.helm.publishing.tests.functional

import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldNotBe
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/**
 * Helper class for tests requiring server interaction.
 *
 * It is reusable, e.g. [close] method just resets state, so you can create one instance of it for class and just close it at the end of each test.
 */
internal class MockServer : AutoCloseable {
    private var server: MockWebServer? = null

    /**
     * Port of the existing server if it runs, exception will be otherwise.
     *
     * Please note, that this method might return different answers, because [close]+[startNew] methods might change server port, because of restart.
     */
    val currentPort: Int
        get() = server!!.port

    fun startNew() {
        require(server == null) {
            "Previous server is running"
        }
        server = MockWebServer().apply {
            start()
        }
    }

    fun enqueue(response: MockResponse) = server!!.enqueue(response)

    /**
     * Validates the earliest request sent and removes it from a queue.
     *
     * Instead of default implementation of [MockWebServer.takeRequest], this method doesn't wait forever
     */
    fun checkRequest(validation: (RecordedRequest) -> Unit) {
        val request = getRequestOrNull()

        request shouldNotBe null

        request.should {
            validation(it!!)
        }
    }

    fun ensureAllRequestsWereChecked() {
        // test might fail before server creation
        if (server == null) {
            return
        }

        val uncheckedRequests = buildList {
            do {
                val request = getRequestOrNull()

                if (request != null) {
                    add(request)
                }
            } while (request != null)
        }

        uncheckedRequests should beEmpty()
    }

    private fun getRequestOrNull() = server!!.takeRequest(0, TimeUnit.MICROSECONDS)

    override fun close() {
        server?.close()

        server = null
    }
}