package com.citi.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.isSuccess
import assertk.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmExtractClient
import com.citi.gradle.plugins.helm.dsl.HelmExtension
import com.citi.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasExtension
import org.unbrokendome.gradle.pluginutils.test.evaluate
import org.unbrokendome.gradle.pluginutils.test.spek.gradleProject


object HelmCommandsPluginTest : Spek({

    val project by gradleProject()


    describe("applying the helm-commands plugin") {

        beforeEachTest {
            project.plugins.apply(HelmCommandsPlugin::class.java)
        }


        it("project can be evaluated successfully") {
            assertThat {
                project.evaluate()
            }.isSuccess()
        }


        it("should create a helm DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
        }


        it("should create a helm lint DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<Linting>("lint")
        }

    }


    describe("plugin should not create any tasks automatically except helmExtractClient") {

        beforeEachTest {
            project.tasks.whenObjectAdded { task ->
                if (task !is HelmExtractClient) {
                    fail("plugin should not add any tasks except helmExtractClient")
                }
            }
        }


        it("should not create any tasks automatically") {
            project.plugins.apply(HelmCommandsPlugin::class.java)
        }
    }
})
