package org.unbrokendome.gradle.plugins.helm.testutil

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.internal.project.ProjectInternal


fun Task.execute(checkUpToDate: Boolean = true) {

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
}


fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
}
