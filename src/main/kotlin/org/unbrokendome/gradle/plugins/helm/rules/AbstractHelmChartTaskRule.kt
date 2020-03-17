package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


internal abstract class AbstractHelmChartTaskRule<T : Task>(
    taskType: Class<T>,
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>,
    namePattern: RuleNamePattern
) : AbstractTaskRule<HelmChart, T>(
    taskType, tasks, charts, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    abstract override fun T.configureFrom(chart: HelmChart)
}
