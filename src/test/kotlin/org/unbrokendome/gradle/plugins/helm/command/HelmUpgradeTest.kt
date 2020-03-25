package org.unbrokendome.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUpgrade
import org.unbrokendome.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import org.unbrokendome.gradle.plugins.helm.spek.applyPlugin
import org.unbrokendome.gradle.plugins.helm.spek.gradleExecMock
import org.unbrokendome.gradle.plugins.helm.spek.gradleTask
import org.unbrokendome.gradle.plugins.helm.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.plugins.helm.testutil.execute


object HelmUpgradeTest : ExecutionResultAwareSpek({

    setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmUpgrade> {
        releaseName.set("awesome-release")
        chart.set("custom/awesome")
    }


    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests,
        ServerOperationOptionsTests("install"),
        InstallFromRepositoryOptionsTests("install")
    ) {

        describe("executing a HelmUpgrade task") {

            it("should execute helm upgrade") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("upgrade")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                }
            }


            it("should use install property") {

                task.install.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("upgrade")
                    expectFlag("--install")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                }
            }


            it("should use reuseValues property") {

                task.reuseValues.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("upgrade")
                    expectFlag("--reuse-values")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                }
            }


            it("should use resetValues property") {

                task.resetValues.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("upgrade")
                    expectFlag("--reset-values")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                }
            }
        }
    }
})
