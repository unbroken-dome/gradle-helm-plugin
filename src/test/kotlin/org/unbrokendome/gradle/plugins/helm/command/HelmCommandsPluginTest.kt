package org.unbrokendome.gradle.plugins.helm.command

import assertk.assert
import assertk.assertions.hasSize
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension


class HelmCommandsPluginTest : AbstractGradleProjectTest() {

    @Test
    fun `Project can be evaluated successfully`() {
        applyPlugin()

        assertDoesNotThrow {
            evaluateProject()
        }
    }


    @Test
    fun `Plugin should create a helm DSL extension`() {
        applyPlugin()

        assert(project, name = "project").hasExtension<HelmExtension>("helm")
    }


    @Test
    fun `Plugin should create a helm lint DSL extension`() {
        applyPlugin()

        assert(project, name = "project")
            .hasExtension<HelmExtension>("helm") {
                it.hasExtension<Linting>("lint")
            }
    }


    @Test
    fun `Plugin should not create any tasks`() {
        val taskCountBefore = project.tasks.size
        applyPlugin()

        assert(project.tasks, name = "tasks")
            .hasSize(taskCountBefore)
    }


    private fun applyPlugin() {
        project.plugins.apply(HelmCommandsPlugin::class.java)
    }
}
