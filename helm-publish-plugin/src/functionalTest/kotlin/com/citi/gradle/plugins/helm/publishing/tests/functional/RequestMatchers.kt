package com.citi.gradle.plugins.helm.publishing.tests.functional

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe
import okhttp3.Credentials
import okhttp3.mockwebserver.RecordedRequest

internal fun haveAuthorization(username: String, password: String): Matcher<RecordedRequest> {
    return object : Matcher<RecordedRequest> {
        val expectedCredentials = Credentials.basic(username, password)
        override fun test(value: RecordedRequest): MatcherResult {
            val authenticationHeader = value.getHeader("Authorization")

            authenticationHeader shouldBe expectedCredentials

            return object : MatcherResult {
                private fun createMessage(wordAfterMust: String): String {
                    return "Authentication header must $wordAfterMust have value $expectedCredentials " +
                            "(equivalent to username=$username, password=$password, however value was $authenticationHeader"
                }

                override fun failureMessage(): String {
                    return createMessage("")
                }

                override fun negatedFailureMessage(): String {
                    return createMessage("not ")
                }

                override fun passed(): Boolean {
                    return authenticationHeader == expectedCredentials
                }
            }
        }

    }
}