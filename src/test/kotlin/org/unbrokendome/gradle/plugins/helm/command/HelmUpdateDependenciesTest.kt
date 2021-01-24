package org.unbrokendome.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUpdateDependencies
import org.unbrokendome.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import org.unbrokendome.gradle.plugins.helm.spek.gradleExecMock
import org.unbrokendome.gradle.plugins.helm.testutil.exec.singleInvocation
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
})
