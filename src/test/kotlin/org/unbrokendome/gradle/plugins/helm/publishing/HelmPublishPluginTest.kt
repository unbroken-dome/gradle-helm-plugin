package org.unbrokendome.gradle.plugins.helm.publishing

import assertk.assertThat
import assertk.assertions.isSuccess
import org.gradle.api.Task
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.spek.applyPlugin
import org.unbrokendome.gradle.plugins.helm.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.containsTask
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.hasExtension
import org.unbrokendome.gradle.plugins.helm.testutil.evaluate


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
