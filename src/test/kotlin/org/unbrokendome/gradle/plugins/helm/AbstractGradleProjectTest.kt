package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


abstract class AbstractGradleProjectTest {

    protected lateinit var project: Project


    @BeforeEach
    fun createProject(testInfo: TestInfo) {

        val annotation = testInfo.testMethod
            .map { it.getAnnotation(GradleProjectName::class.java) }
            .orElse(null)

        project = ProjectBuilder.builder()
            .also { builder ->
                if (annotation != null) {
                    builder.withName(annotation.value)
                }
            }
            .build()
    }


    protected fun evaluateProject() {
        (project as ProjectInternal).evaluate()
    }


    @AfterEach
    fun cleanupProject() {
        project.projectDir.deleteRecursively()
    }
}
