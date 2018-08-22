package org.unbrokendome.gradle.plugins.helm

import assertk.assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.testutil.containsItem
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension
import org.unbrokendome.gradle.plugins.helm.testutil.isPresent


@Suppress("NestedLambdaShadowedImplicitParameter")
object HelmPluginTest : Spek({
    given("a Gradle project") {

        val project = ProjectBuilder.builder().build() as ProjectInternal

        on("applying the plugin") {
            project.plugins.apply(HelmPlugin::class.java)


            it("should have a helm DSL extension") {
                assert(project, name = "project").hasExtension<HelmExtension>("helm")
            }


            it("should have a helm.filtering DSL extension") {
                assert(project, name = "project")
                        .hasExtension<HelmExtension>("helm") {
                            it.hasExtension<Filtering>("filtering")
                        }
            }


            it("should have a helmInitClient task") {
                assert(project.tasks, name = "tasks").containsItem("helmInitClient") {
                    it.isInstanceOf(HelmInit::class) {
                        it.prop(HelmInit::clientOnly).isPresent { it.isTrue() }
                    }
                }
            }


            it("should evaluate the project") {
                project.evaluate()
            }
        }
    }
})
