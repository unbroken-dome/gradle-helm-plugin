package org.unbrokendome.gradle.plugins.helm.testutil.assertions

import assertk.Assert
import org.gradle.api.Task


val Assert<Task>.taskDependencies
    get() = transform { actual ->
        actual.taskDependencies.getDependencies(actual)
    }
