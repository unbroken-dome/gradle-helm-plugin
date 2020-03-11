package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildOrUpdateDependencies
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


private val namePattern =
    RuleNamePattern.parse("helm<Chart>")


/**
 * The name of the [Configuration] that contains the directory artifact for this chart.
 */
val HelmChart.dirArtifactConfigurationName: String
    get() = namePattern.mapName(name)


/**
 * A rule that registers an artifact configuration and an artifact for a chart directory.
 */
internal class ChartDirArtifactRule(
    configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractPatternRule<HelmChart, Configuration>(
    configurations, charts, namePattern
) {
    constructor(project: Project, charts: NamedDomainObjectCollection<HelmChart>)
            : this(project.configurations, project.tasks, charts)


    companion object {
        fun getConfigurationName(chartName: String) =
            namePattern.mapName(chartName)
    }


    override fun Configuration.configureFrom(source: HelmChart) {

        isCanBeResolved = false
        isCanBeConsumed = true

        val buildDependenciesTask =
            tasks.named(source.updateDependenciesTaskName, HelmBuildOrUpdateDependencies::class.java)

        outgoing { publications ->
            publications.artifact(buildDependenciesTask.flatMap { it.chartDir }) { artifact ->
                artifact.builtBy(buildDependenciesTask)
                artifact.name = source.chartName.get()
            }
        }
    }
}
