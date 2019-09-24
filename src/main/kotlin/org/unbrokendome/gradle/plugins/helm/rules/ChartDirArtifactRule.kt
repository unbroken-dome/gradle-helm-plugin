package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildOrUpdateDependencies
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


/**
 * A rule that registers an artifact configuration and an artifact for a chart directory.
 */
internal class ChartDirArtifactRule(
    private val configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    private val artifacts: ArtifactHandler,
    private val charts: Iterable<HelmChart>
) : AbstractRule() {

    constructor(project: Project, charts: Iterable<HelmChart>)
            : this(project.configurations, project.tasks, project.artifacts, charts)


    internal companion object {
        fun getConfigurationName(chartName: String) =
            "helm${chartName.capitalize()}"
    }


    private val regex = Regex(getConfigurationName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
        "Pattern: " + getConfigurationName("<Chart>")


    override fun apply(configurationName: String) {
        if (regex.matches(configurationName)) {
            charts.find { it.dirArtifactConfigurationName == configurationName }
                ?.let { chart ->
                    configurations.create(configurationName)

                    val buildDependenciesTask =
                        tasks.getByName(chart.updateDependenciesTaskName) as HelmBuildOrUpdateDependencies
                    artifacts.add(configurationName, buildDependenciesTask.chartDir) {
                        it.builtBy(buildDependenciesTask)
                        it.name = chart.chartName.get()
                    }
                }
        }
    }
}


/**
 * The name of the [Configuration] that contains the directory artifact for this chart.
 */
val HelmChart.dirArtifactConfigurationName: String
    get() = ChartDirArtifactRule.getConfigurationName(name)
