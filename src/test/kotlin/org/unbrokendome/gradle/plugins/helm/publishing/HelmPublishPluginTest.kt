package org.unbrokendome.gradle.plugins.helm.publishing

import assertk.assert
import assertk.assertions.isNotNull
import org.junit.jupiter.api.*
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmPublishPluginTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPublishPlugin::class.java)
    }


    @Test
    fun `Project can be evaluated successfully`() {
        Assertions.assertDoesNotThrow {
            evaluateProject()
        }
    }


    @Test
    fun `Plugin should create a helm publishing DSL extension`() {
        assert(project, name = "project")
            .hasExtension<HelmExtension>("helm") {
                it.hasExtension<HelmPublishingExtension>("publishing")
            }
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

            val publishAllTask = project.tasks.findByName("helmPublish")
            assert(publishAllTask)
                .isNotNull()
        }
    }
}
