package com.citi.gradle.plugins.helm.publishing.tests.functional

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class OnlyHelmPublishPluginTest {
    private val sourceDirectory = File("./src/functionalTest/resources/test/only-helm-publish-plugin")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @Test
    fun helmPublishPluginCouldBeAppliedAloneButDontCreateAnyTask() {
        // given
        val arguments = listOf("tasks", "--stacktrace")
        val gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments(arguments)

        // when
        val result = gradleRunner.build()

        // then
        val output = result.output

        output shouldContain "BUILD SUCCESSFUL"
        output shouldNotContain "helmPublish"
    }
}
