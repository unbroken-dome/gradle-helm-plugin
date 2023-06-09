package com.citi.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmBuildDependencies
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


object HelmBuildDependenciesTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmBuildDependencies> {
        chartDir.set(project.file("src"))
    }


    withOptionsTesting(GlobalOptionsTests) {

        describe("executing a HelmBuildDependencies task") {

            it("should execute helm dependency build") {

                task.execute(checkOnlyIf = false)

                execMock.singleInvocation {
                    expectCommand("dependency", "build")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }

    describe("when lock file exists (v1 API)") {

        beforeEachTest {
            directory(project.projectDir) {
                directory("src") {
                    file("Chart.yaml", "apiVersion: v1")
                    file("requirements.lock", "")
                }
            }
        }

        describe("executing a HelmBuildDependencies task") {
            it("should execute helm dependency build") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("dependency", "build")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }

    describe("when lock file exists (v2 API)") {

        beforeEachTest {
            directory(project.projectDir) {
                directory("src") {
                    file("Chart.yaml", "apiVersion: v2")
                    file("Chart.lock", "")
                }
            }
        }

        describe("executing a HelmBuildDependencies task") {
            it("should execute helm dependency build") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("dependency", "build")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }

    describe("when lock file does not exist") {

        describe("when chart has no dependencies") {
            beforeEachTest {
                directory(project.projectDir) {
                    directory("src") {
                        file("Chart.yaml",
                            """
                            |apiVersion: v2
                            """.trimMargin())
                    }
                }
            }

            describe("executing a HelmBuildDependencies task") {
                it("should not execute helm dependency build") {

                    val result = task.execute()
                    assertThat(result, "result").isEqualTo(TaskOutcome.SKIPPED)

                    execMock.verifyNoInvocations()
                }
            }
        }

        describe("when chart has external dependencies") {
            beforeEachTest {
                directory(project.projectDir) {
                    directory("src") {
                        file("Chart.yaml",
                            """
                            |apiVersion: v2
                            |dependencies:
                            |  - name: foo
                            |    version: "1.2.3"
                            |    repository: https://my.example.repo
                            """.trimMargin())
                    }
                }
            }

            describe("executing a HelmBuildDependencies task") {
                it("should execute helm dependency build") {

                    task.execute()

                    execMock.singleInvocation {
                        expectCommand("dependency", "build")
                        expectArg("${project.projectDir}/src")
                    }
                }
            }
        }

    }
})
