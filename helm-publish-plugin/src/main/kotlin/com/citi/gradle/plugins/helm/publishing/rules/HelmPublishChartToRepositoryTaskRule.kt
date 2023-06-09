package com.citi.gradle.plugins.helm.publishing.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import com.citi.gradle.plugins.helm.publishing.dsl.publishConvention
import com.citi.gradle.plugins.helm.publishing.tasks.HelmPublishChart
import com.citi.gradle.plugins.helm.rules.packagedArtifactConfigurationName
import org.unbrokendome.gradle.pluginutils.rules.AbstractTaskRule2
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmPublish<Chart>ChartTo<Repo>Repo")


/**
 * Gets the name of the task that publishes this chart to a specific repository.
 *
 * @receiver the [HelmChart]
 * @param repositoryName the name of the repository
 */
internal fun HelmChart.publishToRepositoryTaskName(repositoryName: String) =
    namePattern.mapName(name, repositoryName)


/**
 * Rule that creates a task for publishing a given chart to a given repository.
 */
internal class HelmPublishChartToRepositoryTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>,
    repositories: NamedDomainObjectCollection<HelmPublishingRepository>
) : AbstractTaskRule2<HelmChart, HelmPublishingRepository, HelmPublishChart>(
    HelmPublishChart::class.java, tasks, charts, repositories, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmPublishChart.configureFrom(chart: HelmChart, repository: HelmPublishingRepository) {

        description = "Publishes the ${chart.name} chart to the ${repository.name} repository."

        onlyIf { chart.publishConvention.publish.get() }

        chartName.set(chart.chartName)
        chartVersion.set(chart.chartVersion)
        chartFile.set(
            project.layout.file(
                project.provider {
                    project.configurations.getByName(chart.packagedArtifactConfigurationName)
                        .artifacts.single().file
                })
        )
        targetRepository = repository
        dependsOn(chart)
    }
}
