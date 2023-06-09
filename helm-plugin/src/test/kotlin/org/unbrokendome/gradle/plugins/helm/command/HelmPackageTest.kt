package com.citi.gradle.plugins.helm.command

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmPackage
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.fileValue
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasValueEqualTo
import org.unbrokendome.gradle.pluginutils.test.directory
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmPackageTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmPackage> {
        sourceDir.set(project.file("src"))
        chartVersion.set("1.2.3")
    }


    withOptionsTesting(GlobalOptionsTests) {

        describe("executing a HelmPackage task") {

            it("should execute helm package") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("package")
                    expectOption("--destination", "${project.projectDir}/build/helm/charts")
                    expectOption("--version", "1.2.3")
                    expectArg("${project.projectDir}/src")
                }
            }


            it("should use destinationDir property") {

                task.destinationDir.set(project.file("build/custom-output-dir"))

                task.execute()

                execMock.singleInvocation {
                    expectCommand("package")
                    expectOption("--destination", "${project.projectDir}/build/custom-output-dir")
                    expectOption("--version", "1.2.3")
                    expectArg("${project.projectDir}/src")
                }
            }


            it("should use appVersion property") {

                task.appVersion.set("3.2.1")

                task.execute()

                execMock.singleInvocation {
                    expectCommand("package")
                    expectOption("--destination", "${project.projectDir}/build/helm/charts")
                    expectOption("--version", "1.2.3")
                    expectOption("--app-version", "3.2.1")
                    expectArg("${project.projectDir}/src")
                }
            }


            it("should use updateDependencies property") {

                task.updateDependencies.set(true)

                task.execute()

                execMock.singleInvocation {
                    expectCommand("package")
                    expectOption("--destination", "${project.projectDir}/build/helm/charts")
                    expectOption("--version", "1.2.3")
                    expectFlag("--dependency-update")
                    expectArg("${project.projectDir}/src")
                }
            }


            it("chartFileName returns correct file name") {

                with(task) {
                    chartName.set("awesome-chart")
                    chartVersion.set("1.2.3")
                }

                assertThat(task)
                    .prop(HelmPackage::chartFileName)
                    .hasValueEqualTo("awesome-chart-1.2.3.tgz")
            }


            it("packageFile returns correct path") {

                with(task) {
                    chartName.set("awesome-chart")
                    chartVersion.set("1.2.3")
                }

                assertThat(task)
                    .prop(HelmPackage::packageFile)
                    .fileValue().isEqualTo(project.file("build/helm/charts/awesome-chart-1.2.3.tgz"))
            }


            it("should read chart name and version from Chart yaml") {
                directory("${project.projectDir}/src") {
                    file(
                        "Chart.yaml",
                        contents = """
                        apiVersion: v2
                        name: awesome-chart
                        version: 1.2.3
                    """.trimIndent()
                    )
                }

                assertThat(task).all {
                    prop(HelmPackage::chartName).hasValueEqualTo("awesome-chart")
                    prop(HelmPackage::chartVersion).hasValueEqualTo("1.2.3")
                }
            }
        }
    }
})
