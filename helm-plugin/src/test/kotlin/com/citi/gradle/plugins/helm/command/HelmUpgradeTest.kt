package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmUpgrade
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


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
        InstallationOptionsTests("install"),
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
        }
    }


    describe("executing a HelmUpgrade task") {
        it("should use install property") {

            task.install.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("upgrade")
                expectFlag("--install")
            }
        }


        it("should use reuseValues property") {

            task.reuseValues.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("upgrade")
                expectFlag("--reuse-values")
            }
        }


        it("should use resetValues property") {

            task.resetValues.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("upgrade")
                expectFlag("--reset-values")
            }
        }

        it("should use resetValues property") {

            task.historyMax.set(42)

            task.execute()

            execMock.singleInvocation {
                expectCommand("upgrade")
                expectOption("--history-max", "42")
            }
        }
    }
})
