package org.unbrokendome.gradle.plugins.helm.testutil

import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.workers.WorkerExecutor


fun Task.execute(checkUpToDate: Boolean = true) {

    val services = (project as ProjectInternal).services

    val buildOperationExecutor = services[BuildOperationExecutor::class.java]
    val workerExecutor = services[WorkerExecutor::class.java]

    val buildOperation = buildOperationExecutor.start(BuildOperationDescriptor.displayName(name))

    try {
        if (checkUpToDate) {
            val upToDateSpec = (outputs as TaskOutputsInternal).upToDateSpec
            val upToDate = !upToDateSpec.isEmpty && upToDateSpec.isSatisfiedBy(this as TaskInternal)
            if (upToDate) {
                didWork = false
                return
            }
        }

        actions.forEach {
            it.execute(this)
        }

        workerExecutor.await()

    } finally {
        buildOperation.setResult(BuildResult(project.gradle, null))
    }
}


fun Task.isSkipped(): Boolean {
    this as TaskInternal
    return onlyIf.isSatisfiedBy(this)
}


fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
}
