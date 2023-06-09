package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmLint
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


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
        }
    }

    describe("executing a HelmLint task") {
        it("should use strict property") {

            task.strict.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("lint")
                expectFlag("--strict")
                expectArg("${project.projectDir}/src")
            }
        }

        it("should use withSubcharts property") {

            task.withSubcharts.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("lint")
                expectFlag("--with-subcharts")
                expectArg("${project.projectDir}/src")
            }
        }
    }
})
