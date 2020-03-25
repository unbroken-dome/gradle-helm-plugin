package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Task


class GradleTaskTestContext<T : Task>(
    val taskType: Class<T>,
    val defaultTaskName: String = taskType.simpleName.decapitalize(),
    val defaultConfig: T.() -> Unit = {}
)
