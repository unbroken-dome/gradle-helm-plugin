package com.citi.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmUninstall
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import com.citi.gradle.plugins.helm.testutil.exec.verifyNoInvocations
import org.unbrokendome.gradle.pluginutils.test.TaskOutcome
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmUninstallTest : ExecutionResultAwareSpek({

    setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()
    val lsExecMock by memoized { execMock.forCommand("ls") }
    val commandExecMock by memoized { execMock.forCommand("uninstall") }

    val task by gradleTask<HelmUninstall> {
        releaseName.set("awesome-release")
    }

    fun GradleExecMock.verifyHelmLsInvocation() {
        singleInvocation {
            expectCommand("ls")
            expectOption("-o", "json")
            expectOption("-f", "^\\Qawesome-release\\E$")
        }
    }


    fun LifecycleAware.givenReleaseExists() {
        beforeEachTest {
            lsExecMock.everyExec {
                printsOnStdout(
                    """[{
                          "name": "awesome-release", "namespace": "default",
                          "revision": "42", "updated": "2020-03-15 10:56:45.906903 +0100 CET",
                          "status": "deployed", "chart": "my-repo/awesome-chart",
                          "app_version": "1.2.3"
                        }]""".trimIndent()
                )
            }
        }
        afterEachTest {
            lsExecMock.verifyHelmLsInvocation()
        }
    }


    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests,
        ServerOperationOptionsTests("uninstall")
    ) {

        describe("when the release exists on the server") {

            givenReleaseExists()
            afterEachTest {
                lsExecMock.verifyHelmLsInvocation()
            }

            describe("executing a HelmUninstall task") {

                it("should execute helm uninstall") {

                    task.execute()

                    commandExecMock.singleInvocation {
                        expectCommand("uninstall")
                        expectArg("awesome-release")
                    }
                }
            }
        }
    }

    describe("when the release exists on the server") {

        givenReleaseExists()

        describe("executing a HelmUninstall task") {

            it("should use keepHistory property") {
                task.keepHistory.set(true)

                task.execute()

                commandExecMock.singleInvocation {
                    expectCommand("uninstall")
                    expectFlag("--keep-history")
                    expectArg("awesome-release")
                }
            }

            it("should use wait property") {
                task.wait.set(true)

                task.execute()

                commandExecMock.singleInvocation {
                    expectCommand("uninstall")
                    expectFlag("--wait")
                    expectArg("awesome-release")
                }
            }
        }

    }

    describe("when the release does not exist on the server") {

        beforeEachTest {
            lsExecMock.everyExec {
                printsOnStdout("[]")
            }
        }
        afterEachTest {
            lsExecMock.verifyHelmLsInvocation()
        }

        describe("executing a HelmUninstall task") {

            it("should skip the helm uninstall invocation") {

                val result = task.execute()
                assertThat(result, "result").isEqualTo(TaskOutcome.SKIPPED)

                commandExecMock.verifyNoInvocations()
            }
        }
    }
})
