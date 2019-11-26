package org.unbrokendome.gradle.plugins.helm.command.tasks

import assertk.assertThat
import assertk.assertions.isDirectory
import com.vdurmont.semver4j.Semver
import org.junit.jupiter.api.Assumptions
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
        val versionTask = project.tasks.create("helmVersion", HelmVersion::class.java) { task ->
            task.home.set(project.buildDir.resolve("helm/home"))
        }
        versionTask.helmVersion()

        //skip if we are 3.0.0 or higher
        Assumptions.assumeTrue(versionTask.clientVersion.isLowerThan(HelmVersion.version3))

        val task = project.tasks.create("helmInit", HelmInit::class.java) { task ->
            task.clientOnly.set(true)
            task.home.set(project.buildDir.resolve("helm/home"))
        }

        task.helmInit()

        assertThat(project.buildDir.resolve("helm/home")).isDirectory()
    }
}
