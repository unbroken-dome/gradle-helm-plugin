package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmTemplate
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmTemplateTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }

    val execMock by gradleExecMock()

    val task by gradleTask<HelmTemplate> {
        releaseName.set("awesome-release")
        chart.set("custom/awesome")
        version.set("3.14.5")
        outputDir.set(project.layout.buildDirectory.dir("template-output"))
    }


    withOptionsTesting(
        GlobalOptionsTests,
        GlobalServerOptionsTests,
        ValuesOptionsTests("template"),
        ServerOperationOptionsTests("template"),
        InstallationOptionsTests("template"),
        InstallFromRepositoryOptionsTests("template")
    ) {

        describe("executing a HelmTemplate task") {

            it("should execute helm template") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("template")
                    expectArg("awesome-release")
                    expectArg("custom/awesome")
                    expectOption("--version", "3.14.5")
                    expectOption("--output-dir", "${project.buildDir}/template-output")
                }
            }
        }
    }


    describe("executing a HelmTemplate task") {

        it("should use apiVersions property") {
            task.apiVersions.addAll("v1", "apps/v1", "batch/v1")

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectOption("--api-versions", "v1,apps/v1,batch/v1")
            }
        }

        it("should use replace property") {
            task.replace.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectFlag("--replace")
            }
        }

        it("should use isUpgrade property") {
            task.isUpgrade.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectFlag("--is-upgrade")
            }
        }

        it("should use showOnly property") {
            task.showOnly.addAll("foo.yaml", "bar.yaml")

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectOption("--show-only", "foo.yaml,bar.yaml")
            }
        }

        it("should use validate property") {
            task.validate.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectFlag("--validate")
            }
        }

        it("should use useReleaseName property") {
            task.useReleaseNameInOutputPath.set(true)

            task.execute()

            execMock.singleInvocation {
                expectCommand("template")
                expectFlag("--release-name")
            }
        }
    }
})
