package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmInstall
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmInstallTest : ExecutionResultAwareSpek({

    setupGradleProject { applyPlugin<HelmCommandsPlugin>() }

    val execMock by gradleExecMock()

    val task by gradleTask<HelmInstall> {
        releaseName.set("awesome-release")
        chart.set("custom/awesome")
        version.set("3.14.5")
    }


    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests,
        ServerOperationOptionsTests("install"),
        InstallationOptionsTests("install"),
        InstallFromRepositoryOptionsTests("install")
    ) {

        describe("executing a HelmInstall task") {

            it("should execute helm install") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("install")
                    expectFlag("--install")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                    expectOption("--version", "3.14.5")
                }
            }


            it("should use replace property") {
                task.replace.set(true)

                task.execute()


                execMock.singleInvocation {
                    expectCommand("install")
                    expectFlag("--replace")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                }
            }
        }
    }
})
