package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmTest
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject
import java.time.Duration


object HelmTestTest : ExecutionResultAwareSpek({

    setupGradleProject { applyPlugin<HelmCommandsPlugin>() }

    val execMock by gradleExecMock()

    val task by gradleTask<HelmTest> {
        releaseName.set("awesome-release")
    }

    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests
    ) {

        describe("executing a HelmTest task") {

            it("should execute helm test") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("test")
                    expectArg("awesome-release")
                }
            }


            it("should use showLogs property") {
                task.showLogs.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("test")
                    expectArg("awesome-release")
                    expectFlag("--logs")
                }
            }


            it("should use remoteTimeout property") {
                task.remoteTimeout.set(Duration.ofSeconds(200))

                task.execute()

                execMock.singleInvocation {
                    expectCommand("test")
                    expectArg("awesome-release")
                    expectOption("--timeout", "3m20s")
                }
            }
        }
    }
})
