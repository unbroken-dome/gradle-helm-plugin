package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach


abstract class AbstractGradleProjectTest {

    private var projectBuilderModifier: (ProjectBuilder) -> Unit = { }
    protected lateinit var project: Project


    @BeforeEach
    fun createProject() {
        project = ProjectBuilder.builder()
                .also(projectBuilderModifier)
                .build()
    }


    protected open fun givenProject(spec: ProjectBuilder.() -> Unit) {
        projectBuilderModifier = { it.spec() }
    }


    protected fun evaluateProject() {
        (project as ProjectInternal).evaluate()
    }
}
