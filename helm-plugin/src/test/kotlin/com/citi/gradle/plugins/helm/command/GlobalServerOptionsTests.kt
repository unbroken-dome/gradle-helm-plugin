package com.citi.gradle.plugins.helm.command

import org.gradle.api.Project
import com.citi.gradle.plugins.helm.command.GlobalServerOptionsTests.serverCommands
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.Invocation
import com.citi.gradle.plugins.helm.testutil.exec.eachInvocation


object GlobalServerOptionsTests : AbstractOptionsTests({

    val project: Project by memoized()
    val execMock: GradleExecMock by memoized()

    fun Invocation.isServerCommand(): Boolean =
        this.args.firstOrNull() in serverCommands


    variant("with custom kubeConfig property") {

        beforeEachTest {
            project.helm.kubeConfig.set(project.file("custom-kubeconfig"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectEnvironment("KUBECONFIG", "${project.projectDir}/custom-kubeconfig")
            }
        }
    }


    variant("with custom kubeContext property") {

        beforeEachTest {
            project.helm.kubeContext.set("custom-kubecontext")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectOption("--kube-context", "custom-kubecontext")
            }
        }
    }


    variant("should use namespace property") {

        beforeEachTest {
            project.helm.namespace.set("custom-namespace")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectOption("--namespace", "custom-namespace")
            }
        }
    }
}) {

    private val serverCommands = setOf(
        "get",
        "history", "hist",
        "install",
        "list", "ls",
        "rollback",
        "test",
        "uninstall", "un", "del", "delete",
        "upgrade"
    )
}
