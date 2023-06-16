package com.citi.gradle.plugins.helm.util

import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DataSizeFormatterTest {

    @ParameterizedTest
    @MethodSource("createInputs")
    fun `should format long to readable string`(case: TestCase) {
        val actualOutput = formatDataSize(case.inputNumber)

        actualOutput shouldBe case.expectedText
    }

    @Suppress("UnusedPrivateMember") // used in tests
    private fun createInputs(): Stream<Arguments> {
        return listOf(
            TestCase(0L, "0.0 B"),
            TestCase(1L, "1.0 B"),
            TestCase(2000L, "2.0 KB"),
            TestCase(3_000_000L, "2.9 MB"),
            TestCase(4_000_000_000, "3.7 GB"),
            TestCase(5_000_000_000_000, "4656.6 GB")
        ).map { Arguments.arguments(it) }
            .stream()
    }

    data class TestCase(val inputNumber: Long, val expectedText: String)
}
