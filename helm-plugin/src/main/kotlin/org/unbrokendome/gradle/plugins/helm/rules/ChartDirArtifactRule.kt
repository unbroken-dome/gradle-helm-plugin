package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.pluginutils.rules.AbstractPatternRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


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
class ChartDirArtifactRule(
    configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractPatternRule<HelmChart, Configuration>(
    configurations, charts, namePattern
) {

    companion object {
        fun getConfigurationName(chartName: String) =
            namePattern.mapName(chartName)
    }


    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun Configuration.configureFrom(chart: HelmChart) {

        isCanBeResolved = false
        isCanBeConsumed = true

        outgoing { publications ->
            publications.artifact(chart.outputDir) { artifact ->
                artifact.builtBy(tasks.named(chart.updateDependenciesTaskName))
                artifact.name = chart.chartName.get()
            }
        }
    }
}
