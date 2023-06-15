package com.citi.gradle.plugins.helm.publishing.tests.functional

import com.citi.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import com.citi.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class OnlyHelmPublishPluginTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/only-helm-publish-plugin")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource("com.citi.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSet")
    fun helmPublishPluginCouldBeAppliedAloneButDontCreateAnyTask(parameters: DefaultGradleRunnerParameters) {
        // given
        val arguments = listOf("tasks", "--stacktrace")
        val gradleRunner = GradleRunnerProvider
            .createRunner(parameters)
            .withProjectDir(testProjectDir)
            .withArguments(arguments)

        // when
        val result = gradleRunner.build()

        // then
        val output = result.output

        output shouldContain "BUILD SUCCESSFUL"
        output shouldNotContain "helmPublish"
    }
}
