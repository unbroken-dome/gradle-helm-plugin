package org.unbrokendome.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUninstall
import org.unbrokendome.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import org.unbrokendome.gradle.plugins.helm.spek.applyPlugin
import org.unbrokendome.gradle.plugins.helm.spek.gradleExecMock
import org.unbrokendome.gradle.plugins.helm.spek.gradleTask
import org.unbrokendome.gradle.plugins.helm.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.plugins.helm.testutil.execute


object HelmUninstallTest : ExecutionResultAwareSpek({

    setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()
    val lsExecMock by memoized { execMock.forCommand("ls") }
    val commandExecMock by memoized { execMock.forCommand("uninstall") }

    val task by gradleTask<HelmUninstall> {
        releaseName.set("awesome-release")
    }


    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests,
        ServerOperationOptionsTests("uninstall")
    ) {

        describe("when the release exists on the server") {

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
                lsExecMock.singleInvocation() {
                    expectCommand("ls")
                    expectOption("-o", "json")
                    expectOption("-f", "^\\Qawesome-release\\E$")
                }
            }


            describe("executing a HelmUninstall task") {

                it("should execute helm uninstall") {

                    task.execute()

                    commandExecMock.singleInvocation {
                        expectCommand("uninstall")
                        expectArg("awesome-release")
                    }
                }


                it("should use keepHistory property") {
                    task.keepHistory.set(true)

                    task.execute()

                    commandExecMock.singleInvocation {
                        expectCommand("uninstall")
                        expectFlag("--keep-history")
                        expectArg("awesome-release")
                    }
                }
            }
        }
    }
})
