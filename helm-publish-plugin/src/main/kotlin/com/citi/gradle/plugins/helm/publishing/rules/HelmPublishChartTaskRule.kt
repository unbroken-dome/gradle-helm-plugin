package com.citi.gradle.plugins.helm.publishing.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import com.citi.gradle.plugins.helm.publishing.dsl.publishConvention
import com.citi.gradle.plugins.helm.rules.AbstractHelmChartTaskRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmPublish<Chart>Chart")


/**
 * The name of the task that publishes this chart to all repositories.
 */
val HelmChart.publishTaskName: String
    get() = namePattern.mapName(name)


/**
 * Rule that creates a task for publishing a given chart to all known repositories.
 */
internal class HelmPublishChartTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>,
    private val repositories: Iterable<HelmPublishingRepository>
) : AbstractHelmChartTaskRule<Task>(Task::class.java, tasks, charts, namePattern) {

    override fun Task.configureFrom(chart: HelmChart) {
        group = HELM_GROUP
        description = "Publishes the ${chart.name} chart."
        onlyIf { chart.publishConvention.publish.get() }

        dependsOn(TaskDependency {
            repositories.map { repository ->
                tasks.getByName(chart.publishToRepositoryTaskName(repository.name))
            }.toSet()
        })
    }
}
