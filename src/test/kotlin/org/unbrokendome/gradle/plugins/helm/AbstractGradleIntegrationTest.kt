package org.unbrokendome.gradle.plugins.helm

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.unbrokendome.gradle.plugins.helm.testutil.directory
import java.io.File
import java.nio.file.Files


abstract class AbstractGradleIntegrationTest {

    protected lateinit var projectDir: File

    protected val buildDir: File
        get() = projectDir.resolve("build")


    @BeforeEach
    fun setupProject(testInfo: TestInfo) {
        projectDir = Files.createTempDirectory("gradle").toFile()

        val annotation = testInfo.testMethod
            .map { it.getAnnotation(GradleProjectName::class.java) }
            .orElse(null)
        val projectName = annotation?.value ?: projectDir.name

        // Always create a settings file, otherwise Gradle searches up the directory hierarchy
        // (and might actually find another file)
        directory(projectDir) {
            file(
                "settings.gradle", contents = """ 
                rootProject.name = '$projectName'
                
                """
            )
        }
    }


    @AfterEach
    fun cleanupProject() {
        projectDir.deleteRecursively()
    }


    protected fun runGradle(vararg args: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(listOf(*args) + "--stacktrace")
            .build()


    protected fun Assert<BuildResult>.taskOutcome(taskPath: String) = transform { actual ->
        val buildTask = actual.task(taskPath)
            ?: expected("task \"$taskPath\" to be executed",
                expected = "task \"$taskPath\" was executed in Gradle build",
                actual = "all executed tasks: [${actual.tasks.joinToString { it.path }}]"
            )
        buildTask.outcome
    }


    protected fun Assert<TaskOutcome>.isSuccess() = isEqualTo(TaskOutcome.SUCCESS)
}
