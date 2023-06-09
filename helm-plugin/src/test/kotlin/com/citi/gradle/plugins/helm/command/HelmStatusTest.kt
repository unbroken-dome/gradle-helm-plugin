package com.citi.gradle.plugins.helm.command

import assertk.all
import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.hasText
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmStatus
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmStatusTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }

    val execMock by gradleExecMock()

    val task by gradleTask<HelmStatus> {
        releaseName.set("awesome-release")
    }

    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests
    ) {

        describe("executing a HelmStatus task") {

            it("should execute helm status") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("status")
                    expectArg("awesome-release")
                }
            }


            it("should use revision property") {
                task.revision.set(42)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("status")
                    expectArg("awesome-release")
                    expectOption("--revision", "42")
                }
            }


            it("should use outputFormat property") {
                task.outputFormat.set("json")

                task.execute()

                execMock.singleInvocation {
                    expectCommand("status")
                    expectArg("awesome-release")
                    expectOption("--output", "json")
                }
            }
        }
    }


    describe("output to file") {

        it("should use outputFile property") {
            val sampleOutput = "Sample status"
            val outputFile = project.file("status-output")
            task.outputFile.set(outputFile)

            execMock.forCommand("status")
                .everyExec {
                    printsOnStdout(sampleOutput)
                }

            task.execute()

            execMock.singleInvocation {
                expectCommand("status")
                expectArg("awesome-release")
            }

            assertThat(outputFile, name = "output file").all {
                exists()
                hasText(sampleOutput)
            }
        }

        val extensionsToFormats = mapOf("json" to "json", "yaml" to "yaml", "yml" to "yaml")
        for ((extension, outputFormat) in extensionsToFormats) {

            it("should use $outputFormat format as default if outputFile has .$extension extension") {
                val sampleOutput = "Sample status"
                val outputFile = project.file("status-output.$extension")
                task.outputFile.set(outputFile)

                execMock.forCommand("status")
                    .everyExec {
                        printsOnStdout(sampleOutput)
                    }

                task.execute()

                execMock.singleInvocation {
                    expectCommand("status")
                    expectArg("awesome-release")
                    expectOption("--output", outputFormat)
                }

                assertThat(outputFile, name = "output file").all {
                    exists()
                    hasText(sampleOutput)
                }
            }
        }
    }
})
