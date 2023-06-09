package com.citi.gradle.plugins.helm.publishing

import assertk.assertThat
import assertk.assertions.isSuccess
import org.gradle.api.Task
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.HelmPlugin
import com.citi.gradle.plugins.helm.dsl.HelmExtension
import com.citi.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsTask
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasExtension
import org.unbrokendome.gradle.pluginutils.test.evaluate
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmPublishPluginTest : Spek({

    val project by setupGradleProject { applyPlugin<HelmPublishPlugin>() }


    describe("applying the helm-publish plugin") {

        it("project can be evaluated successfully") {
            assertThat {
                project.evaluate()
            }.isSuccess()
        }


        it("should create a helm publishing DSL extension") {
            assertThat(project, name = "project")
                .hasExtension<HelmExtension>("helm")
                .hasExtension<HelmPublishingExtension>("publishing")
        }


        describe("with helm plugin") {

            beforeEachTest {
                project.plugins.apply(HelmPlugin::class.java)
            }


            it("should create a helmPublish task") {
                project.evaluate()

                assertThat(project, name = "project")
                    .containsTask<Task>("helmPublish")
            }
        }
    }
})
