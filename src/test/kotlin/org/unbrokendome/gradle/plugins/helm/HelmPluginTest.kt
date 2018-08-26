package org.unbrokendome.gradle.plugins.helm

import assertk.assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.gradle.api.NamedDomainObjectContainer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.HelmRepository
import org.unbrokendome.gradle.plugins.helm.testutil.containsItem
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension
import org.unbrokendome.gradle.plugins.helm.testutil.isPresent


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmPluginTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Project can be evaluated successfully`() {
        assertDoesNotThrow {
            evaluateProject()
        }
    }


    @Test
    fun `Plugin should create a helm DSL extension`() {
        assert(project, name = "project").hasExtension<HelmExtension>("helm")
    }


    @Test
    fun `Plugin should create a helm repositories DSL extension`() {
        assert(project, name = "project")
                .hasExtension<HelmExtension>("helm") {
                    it.hasExtension<NamedDomainObjectContainer<HelmRepository>>("repositories")
                }
    }


    @Test
    fun `Plugin should create a helm filtering DSL extension`() {
        assert(project, name = "project")
                .hasExtension<HelmExtension>("helm") {
                    it.hasExtension<Filtering>("filtering")
                }
    }


    @Test
    fun `Plugin should create a helm charts DSL extension`() {
        assert(project, name = "project")
                .hasExtension<HelmExtension>("helm") {
                    it.hasExtension<NamedDomainObjectContainer<HelmChart>>("charts")
                }
    }


    @Test
    fun `Plugin should create a helmInitClient task`() {
        assert(project.tasks, name = "tasks").containsItem("helmInitClient") {
            it.isInstanceOf(HelmInit::class) {
                it.prop(HelmInit::clientOnly).isPresent { it.isTrue() }
            }
        }
    }
}
