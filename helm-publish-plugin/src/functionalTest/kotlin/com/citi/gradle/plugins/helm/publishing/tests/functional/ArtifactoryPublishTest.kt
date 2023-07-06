package com.citi.gradle.plugins.helm.publishing.tests.functional

import com.citi.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import com.citi.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.net.HttpURLConnection
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class ArtifactoryPublishTest {
    private val sourceDirectory = File("./src/functionalTest/resources/test/artifactory-publish")

    private val mockServer = MockServer()

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)

        mockServer.startNew()
    }

    @AfterEach
    fun tearDown() {
        mockServer.ensureAllRequestsWereChecked()
        mockServer.close()
    }

    @ParameterizedTest
    @MethodSource("com.citi.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSet")
    fun helmPublishShouldCallArtifactoryWithCredentials(parameters: DefaultGradleRunnerParameters) {
        // given
        val destinationChartArchive =
            File(testProjectDir, "build\\helm\\charts\\${testProjectDir.name}-unspecified.tgz")
        val helmExecutableParameter =
            HelmExecutable.getExecutableParameterForChartCreation(testProjectDir, destinationChartArchive)

        val mockedResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
        mockServer.enqueue(mockedResponse)

        val arguments = listOf(
            "helmPublish",
            "--stacktrace",
            helmExecutableParameter.parameterValue,
            "-PserverPort=${mockServer.currentPort}"
        )
        val gradleRunner = GradleRunnerProvider
            .createRunner(parameters)
            .withProjectDir(testProjectDir)
            .withArguments(arguments)

        // when
        val result = gradleRunner.build()

        // then
        val output = result.output

        output shouldContain "BUILD SUCCESSFUL"
        output shouldContain "helmPublish"

        mockServer.checkRequest { recordedRequest ->
            recordedRequest should haveAuthorization(username = "testUserName", password = "testPassword")
            // expected path is folder path requested plus helm chart name (e.g. project name and version)
            recordedRequest.path shouldBe "/artifactory/folder1/folder2/${testProjectDir.name}-unspecified.tgz"
        }
    }
}
