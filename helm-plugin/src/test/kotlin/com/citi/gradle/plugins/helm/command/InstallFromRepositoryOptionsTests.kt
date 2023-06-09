package com.citi.gradle.plugins.helm.command

import org.gradle.api.Project
import org.gradle.api.Task
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.Invocation
import com.citi.gradle.plugins.helm.testutil.exec.eachInvocation
import java.net.URI


class InstallFromRepositoryOptionsTests(vararg commands: String) : AbstractOptionsTests({

    val project: Project by memoized()
    val task: Task by memoized()
    val options by memoized { task as ConfigurableHelmInstallFromRepositoryOptions }
    val execMock: GradleExecMock by memoized()

    fun Invocation.matchesCommand(): Boolean =
        args.firstOrNull() in commands


    variant("with repository property") {

        beforeEachTest {
            options.repository.set(URI.create("http://helm-charts.example.com"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--repo", "http://helm-charts.example.com")
            }
        }
    }


    variant("with username and password properties") {

        beforeEachTest {
            options.username.set("john.doe")
            options.password.set("topsecret")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--username", "john.doe")
                expectOption("--password", "topsecret")
            }
        }
    }


    variant("with caFile property") {

        beforeEachTest {
            options.caFile.set(project.file("ca.pem"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--ca-file", "${project.projectDir}/ca.pem")
            }
        }
    }


    variant("with certFile and keyFile properties") {

        beforeEachTest {
            options.certFile.set(project.file("cert.pem"))
            options.keyFile.set(project.file("key.pem"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--cert-file", "${project.projectDir}/cert.pem")
                expectOption("--key-file", "${project.projectDir}/key.pem")
            }
        }
    }
})
