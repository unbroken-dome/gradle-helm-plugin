package org.unbrokendome.gradle.plugins.helm.command

import assertk.assert
import assertk.assertions.hasSize
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension


object HelmCommandsPluginTest : Spek({
    given("a Gradle project") {

        val project = ProjectBuilder.builder().build() as ProjectInternal
        val taskCountBefore = project.tasks.size

        on("applying the plugin") {
            project.plugins.apply(HelmCommandsPlugin::class.java)


            it("should have a helm DSL extension") {
                assert(project, name = "project").hasExtension<HelmExtension>("helm")
            }


            it("should have a helm.lint DSL extension") {
                assert(project, name = "project")
                        .hasExtension<HelmExtension>("helm") {
                            it.hasExtension<Linting>("lint")
                        }
            }


            it("should successfully evaluate the project") {
                println(project.hashCode())
                project.evaluate()
            }


            it("should not create any tasks") {
                println(project.hashCode())
                project.evaluate()
                assert(project.tasks, name = "tasks")
                        .hasSize(taskCountBefore)
            }
        }
    }
})
