package org.unbrokendome.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isSuccess
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.hasExtension


class HelmCommandsPluginTest : AbstractGradleProjectTest() {

    @Test
    fun `Project can be evaluated successfully`() {
        applyPlugin()

        assertThat { evaluateProject() }.isSuccess()
    }


    @Test
    fun `Plugin should create a helm DSL extension`() {
        applyPlugin()

        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
    }


    @Test
    fun `Plugin should create a helm lint DSL extension`() {
        applyPlugin()

        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
            .hasExtension<Linting>("lint")
    }


    @Test
    fun `Plugin should not create any tasks`() {
        val taskCountBefore = project.tasks.size

        applyPlugin()

        assertThat(this::project)
            .prop("tasks") { it.tasks }
            .hasSize(taskCountBefore)
    }


    private fun applyPlugin() {
        project.plugins.apply(HelmCommandsPlugin::class.java)
    }
}
