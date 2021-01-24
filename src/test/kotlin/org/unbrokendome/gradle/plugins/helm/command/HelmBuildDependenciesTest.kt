package org.unbrokendome.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildDependencies
import org.unbrokendome.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import org.unbrokendome.gradle.plugins.helm.spek.gradleExecMock
import org.unbrokendome.gradle.plugins.helm.testutil.exec.singleInvocation
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

                task.execute()

                execMock.singleInvocation {
                    expectCommand("dependency", "build")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }
})
