package org.unbrokendome.gradle.plugins.helm.command.tasks

import assertk.assert
import assertk.assertions.isDirectory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin

class HelmInitTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyHelmCommandsPlugin() {
        project.plugins.apply(HelmCommandsPlugin::class.java)
    }


    @Test
    fun `should run helmInit`() {

        val task = project.tasks.create("helmInit", HelmInit::class.java) { task ->
            task.clientOnly.set(true)
            task.home.set(project.buildDir.resolve("helm/home"))
        }

        task.helmInit()

        assert(project.buildDir.resolve("helm/home")).isDirectory()
    }
}
