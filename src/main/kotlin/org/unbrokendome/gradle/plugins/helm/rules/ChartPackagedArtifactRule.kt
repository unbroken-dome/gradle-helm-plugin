package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage


/**
 * A rule that registers an artifact configuration and an artifact for a chart package.
 *
 * The artifact will contain a single file, which is the tar.gz package file of the chart.
 */
internal class ChartPackagedArtifactRule(
        private val configurations: ConfigurationContainer,
        private val tasks: TaskContainer,
        private val artifacts: ArtifactHandler,
        private val charts: Iterable<HelmChart>)
    : AbstractRule() {

    constructor(project: Project, charts: Iterable<HelmChart>)
            : this(project.configurations, project.tasks, project.artifacts, charts)


    internal companion object {
        fun getConfigurationName(chartName: String) =
                "helm${chartName.capitalize()}Packaged"
    }


    private val regex = Regex(getConfigurationName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
            "Pattern: " + getConfigurationName("<Chart>")


    override fun apply(configurationName: String) {
        if (regex.matches(configurationName)) {
            charts.find { it.packagedArtifactConfigurationName == configurationName }
                    ?.let { chart ->
                        configurations.create(configurationName)

                        val packageTask = tasks.getByName(chart.packageTaskName) as HelmPackage

                        artifacts.add(configurationName, packageTask.chartOutputPath) {
                            it.builtBy(packageTask)
                            it.name = chart.chartName.get()
                            it.extension = "tgz"
                        }
                    }
        }
    }
}


/**
 * The name of the [Configuration] that contains the package artifact for this chart.
 */
val HelmChart.packagedArtifactConfigurationName: String
    get() = ChartPackagedArtifactRule.getConfigurationName(name)
