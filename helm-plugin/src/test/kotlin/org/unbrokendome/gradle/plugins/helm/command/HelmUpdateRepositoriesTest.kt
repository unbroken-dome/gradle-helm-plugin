package com.citi.gradle.plugins.helm.command

import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmUpdateRepositories
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.spek.ExecutionResultAwareSpek
import com.citi.gradle.plugins.helm.spek.gradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.singleInvocation
import com.citi.gradle.plugins.helm.testutil.exec.verifyNoInvocations
import org.unbrokendome.gradle.pluginutils.test.directory
import org.unbrokendome.gradle.pluginutils.test.execute
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.gradleTask
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmUpdateRepositoriesTest : ExecutionResultAwareSpek({

    val project by setupGradleProject { applyPlugin<HelmCommandsPlugin>() }
    val execMock by gradleExecMock()

    val task by gradleTask<HelmUpdateRepositories> {
        repositoryNames.add("stable")
    }


    fun createRepositoriesFile() {
        directory(project.file(project.helm.xdgConfigHome.dir("helm"))) {
            // The file can be empty, the task will only check for its existence but won't read it
            file("repositories.yaml", contents = "")
        }
    }


    withOptionsTesting(GlobalOptionsTests) {

        describe("executing a HelmUpdateRepositories task") {

            it("should skip if there are no repositories") {

                task.repositoryNames.set(emptyList())

                task.execute()

                execMock.verifyNoInvocations()
            }
        }


        describe("with repositories file") {

            beforeEachTest {
                createRepositoriesFile()
            }


            it("should execute helm repo update") {

                task.execute()

                execMock.singleInvocation {
                    expectCommand("repo", "update")
                }
            }
        }
    }


    describe("with repositories file") {

        beforeEachTest {
            createRepositoriesFile()
        }


        it("should execute helm repo update only once for two tasks") {

            task.doLast {
                // Create the two cache files to simulate what helm repo update is doing
                directory(project.file(".gradle/helm/cache/helm/repository")) {
                    file("stable-charts.txt", contents = "")
                    file("stable-index.yaml", contents = "")
                }
            }

            val secondTask =
                project.tasks.create("helmUpdateRepositories2", HelmUpdateRepositories::class.java) { task ->
                    task.repositoryNames.add("stable")
                }

            task.execute()
            secondTask.execute()

            execMock.singleInvocation {
                expectCommand("repo", "update")
            }
        }
    }
})
