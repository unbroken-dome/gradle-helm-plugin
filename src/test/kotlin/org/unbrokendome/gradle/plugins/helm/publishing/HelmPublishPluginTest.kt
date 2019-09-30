package org.unbrokendome.gradle.plugins.helm.publishing

import assertk.assertThat
import assertk.assertions.isSuccess
import org.gradle.api.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.containsTask
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.hasExtension


class HelmPublishPluginTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPublishPlugin::class.java)
    }


    @Test
    fun `Project can be evaluated successfully`() {
        assertThat { evaluateProject() }.isSuccess()
    }


    @Test
    fun `Plugin should create a helm publishing DSL extension`() {
        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
            .hasExtension<HelmPublishingExtension>("publishing")
    }


    @Nested
    @DisplayName("with helm plugin")
    inner class WithHelmPlugin {

        @BeforeEach
        fun applyHelmPlugin() {
            project.plugins.apply(HelmPlugin::class.java)
        }


        @Test
        fun `plugin should create a helmPublish task`() {
            evaluateProject()

            assertThat(this@HelmPublishPluginTest::project)
                .containsTask<Task>("helmPublish")
        }
    }
}
