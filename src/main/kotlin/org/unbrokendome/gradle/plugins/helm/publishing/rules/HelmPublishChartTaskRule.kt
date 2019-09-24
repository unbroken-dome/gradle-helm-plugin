package org.unbrokendome.gradle.plugins.helm.publishing.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.publishConvention
import org.unbrokendome.gradle.plugins.helm.rules.AbstractRule


/**
 * Rule that creates a task for publishing a given chart to all known repositories.
 */
internal class HelmPublishChartTaskRule(
    private val project: Project,
    private val charts: NamedDomainObjectCollection<HelmChart>,
    private val repositories: Iterable<HelmPublishingRepository>
) : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String) =
            "helmPublish${chartName.capitalize()}Chart"
    }

    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
        "Pattern: " + getTaskName("<Chart>")


    override fun apply(taskName: String) {

        regex.matchEntire(taskName)?.let { matchResult ->
            val chartName = matchResult.groupValues[1].decapitalize()
            val chart = charts.findByName(chartName) ?: charts.findByName(chartName.capitalize())

            if (chart != null) {

                val taskDependency = TaskDependency {
                    repositories.map { repository ->
                        project.tasks.getByName(chart.publishToRepositoryTaskName(repository.name))
                    }.toSet()
                }

                project.tasks.create(taskName) { task ->
                    task.group = HELM_GROUP
                    task.description = "Publishes the ${chart.name} chart."
                    task.onlyIf { chart.publishConvention.publish.get() }
                    task.dependsOn(taskDependency)
                }
            }
        }
    }
}


/**
 * The name of the task that publishes this chart to all repositories.
 */
val HelmChart.publishTaskName: String
    get() = HelmPublishChartTaskRule.getTaskName(name)
