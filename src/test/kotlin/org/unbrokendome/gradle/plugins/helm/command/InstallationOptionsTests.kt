package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Task
import org.unbrokendome.gradle.plugins.helm.testutil.exec.GradleExecMock
import org.unbrokendome.gradle.plugins.helm.testutil.exec.Invocation
import org.unbrokendome.gradle.plugins.helm.testutil.exec.eachInvocation


class InstallationOptionsTests(vararg commands: String) : AbstractOptionsTests({

    val task: Task by memoized()
    val options by memoized { task as ConfigurableHelmInstallationOptions }
    val execMock: GradleExecMock by memoized()


    fun Invocation.matchesCommand(): Boolean =
        args.firstOrNull() in commands


    variant("with atomic property") {

        beforeEachTest {
            options.atomic.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--atomic")
            }
        }
    }


    variant("with devel property") {

        beforeEachTest {
            options.devel.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--devel")
            }
        }
    }


    variant("with verify property") {

        beforeEachTest {
            options.verify.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--verify")
            }
        }
    }


    variant("with wait property") {

        beforeEachTest {
            options.wait.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--wait")
            }
        }
    }


    variant("with wait property") {

        beforeEachTest {
            options.version.set("1.2.3")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--version", "1.2.3")
            }
        }
    }


    variant("with createNamespace property") {

        beforeEachTest {
            options.createNamespace.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--create-namespace")
            }
        }
    }
})
