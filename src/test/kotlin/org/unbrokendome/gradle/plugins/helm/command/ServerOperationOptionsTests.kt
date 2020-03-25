package org.unbrokendome.gradle.plugins.helm.command

import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmServerOperationCommandTask
import org.unbrokendome.gradle.plugins.helm.testutil.exec.GradleExecMock
import org.unbrokendome.gradle.plugins.helm.testutil.exec.Invocation
import org.unbrokendome.gradle.plugins.helm.testutil.exec.eachInvocation
import java.time.Duration


class ServerOperationOptionsTests(vararg commands: String) : AbstractOptionsTests({

    val task: AbstractHelmServerOperationCommandTask by memoized()
    val execMock: GradleExecMock by memoized()

    fun Invocation.matchesCommand(): Boolean =
        args.firstOrNull() in commands


    variant("with dryRun property") {
        beforeEachTest {
            task.dryRun.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--dry-run")
            }
        }
    }


    variant("with noHooks property") {
        beforeEachTest {
            task.noHooks.set(true)
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectFlag("--no-hooks")
            }
        }
    }


    variant("with remoteTimeout property") {
        beforeEachTest {
            task.remoteTimeout.set(Duration.ofSeconds(200))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--timeout", "3m20s")
            }
        }
    }
})
