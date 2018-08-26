package org.unbrokendome.gradle.plugins.helm.publishing.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.publishConvention
import org.unbrokendome.gradle.plugins.helm.publishing.tasks.HelmPublishChart
import org.unbrokendome.gradle.plugins.helm.rules.AbstractRule
import org.unbrokendome.gradle.plugins.helm.rules.dirArtifactConfigurationName


/**
 * Rule that creates a task for publishing a given chart to a given repository.
 */
internal class HelmPublishChartToRepositoryTaskRule(
        private val project: Project,
        private val charts: NamedDomainObjectCollection<HelmChart>,
        private val repositories: NamedDomainObjectCollection<HelmPublishingRepository>
) : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String, repositoryName: String) =
                "helmPublish${chartName.capitalize()}ChartTo${repositoryName.capitalize()}Repo"
    }

    private val regex = Regex(getTaskName("(\\p{Upper}.*)", "(\\p{Upper}.*)"))


    override fun getDescription(): String =
            "Pattern: ${getTaskName("<Chart>", "<Repo>")}"


    override fun apply(taskName: String) {

        regex.matchEntire(taskName)?.let { matchResult ->
            val chartName = matchResult.groupValues[1].decapitalize()
            val chart = charts.findByName(chartName) ?: charts.findByName(chartName.capitalize())

            val repositoryName = matchResult.groupValues[2].decapitalize()
            val repository = repositories.findByName(repositoryName) ?: repositories.findByName(repositoryName.capitalize())

            if (chart != null && repository != null) {

                project.tasks.create(taskName, HelmPublishChart::class.java) { task ->
                    task.description = "Publishes the ${chart.name} chart to the ${repository.name} repository."

                    task.onlyIf { chart.publishConvention.publish.get() }

                    task.chartFile.set(project.layout.file(
                            project.provider {
                                project.configurations.getByName(chart.dirArtifactConfigurationName)
                                        .singleFile
                            }))
                    task.targetRepository = repository
                    task.dependsOn(chart)
                }
            }
        }
    }
}


/**
 * Gets the name of the task that publishes this chart to a specific repository.
 *
 * @receiver the [HelmChart]
 * @param repositoryName the name of the repository
 */
internal fun HelmChart.publishToRepositoryTaskName(repositoryName: String) =
        HelmPublishChartToRepositoryTaskRule.getTaskName(name, repositoryName)
