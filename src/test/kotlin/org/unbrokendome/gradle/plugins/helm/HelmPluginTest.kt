package org.unbrokendome.gradle.plugins.helm

import assertk.assertThat
import assertk.assertions.isSuccess
import org.gradle.api.NamedDomainObjectContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.HelmRepository
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.hasExtension


class HelmPluginTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Project can be evaluated successfully`() {
        assertThat { evaluateProject() }.isSuccess()
    }


    @Test
    fun `Plugin should create a helm DSL extension`() {
        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
    }


    @Test
    fun `Plugin should create a helm repositories DSL extension`() {
        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
            .hasExtension<NamedDomainObjectContainer<HelmRepository>>("repositories")
    }


    @Test
    fun `Plugin should create a helm filtering DSL extension`() {
        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
            .hasExtension<Filtering>("filtering")
    }


    @Test
    fun `Plugin should create a helm charts DSL extension`() {
        assertThat(this::project)
            .hasExtension<HelmExtension>("helm")
            .hasExtension<NamedDomainObjectContainer<HelmChart>>("charts")
    }
}
