package org.unbrokendome.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmLint
import org.unbrokendome.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import org.unbrokendome.gradle.plugins.helm.spek.applyPlugin
import org.unbrokendome.gradle.plugins.helm.spek.gradleExecMock
import org.unbrokendome.gradle.plugins.helm.spek.gradleTask
import org.unbrokendome.gradle.plugins.helm.spek.setupGradleProject
import org.unbrokendome.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.plugins.helm.testutil.execute


object HelmLintTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmLint> {
        chartDir.set(project.file("src"))
    }


    withOptionsTesting(
        GlobalOptionsTests,
        ValuesOptionsTests("lint")
    ) {

        describe("executing a HelmLint task") {

            it("should execute helm lint") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("lint")
                    expectArg("${project.projectDir}/src")
                }
            }


            it("should use strict property") {

                task.strict.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("lint")
                    expectFlag("--strict")
                    expectArg("${project.projectDir}/src")
                }
            }
        }
    }
})
