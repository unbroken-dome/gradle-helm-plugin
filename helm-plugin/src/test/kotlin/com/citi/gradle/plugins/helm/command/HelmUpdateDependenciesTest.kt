package com.citi.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmUpdateDependencies
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import com.citi.gradle.plugins.helm.testutil.exec.verifyNoInvocations
import org.unbrokendome.gradle.pluginutils.test.TaskOutcome
import org.unbrokendome.gradle.pluginutils.test.directory
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmUpdateDependenciesTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmUpdateDependencies> {
        chartDir.set(project.file("src"))
    }


    withOptionsTesting(GlobalOptionsTests) {

        describe("executing a HelmUpdateDependencies task") {

            it("should execute helm dependency update") {

                task.execute(checkOnlyIf = false)

                execMock.singleInvocation {
                    expectCommand("dependency", "update")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }


    describe("when chart has external dependencies") {

        beforeEachTest {
            directory(project.projectDir) {
                directory("src") {
                    file(
                        "Chart.yaml",
                        """
                            |apiVersion: v2
                            |dependencies:
                            |  - name: foo
                            |    version: "1.2.3"
                            |    repository: https://my.example.repo
                            """.trimMargin()
                    )
                }
            }
        }

        describe("when executing a HelmUpdateDependencies task") {

            it("should execute helm dependency update") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("dependency", "update")
                    expectArg("${project.projectDir}/src")
                }
            }

            it("should use skipRefresh property") {

                task.skipRefresh.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("dependency", "update")
                    expectFlag("--skip-refresh")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }

    describe("when chart has no external dependencies") {

        beforeEachTest {
            directory(project.projectDir) {
                directory("src") {
                    file(
                        "Chart.yaml",
                        """
                            |apiVersion: v2
                            """.trimMargin()
                    )
                }
            }
        }

        it("should skip helm dependency update") {

            val result = task.execute()
            assertThat(result, "result").isEqualTo(TaskOutcome.SKIPPED)

            execMock.verifyNoInvocations()
        }
    }
})
