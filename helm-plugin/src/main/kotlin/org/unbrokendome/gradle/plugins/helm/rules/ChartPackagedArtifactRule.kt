package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.pluginutils.rules.AbstractPatternRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helm<Chart>Packaged")


/**
 * The name of the [Configuration] that contains the packaged artifact for this chart.
 */
val HelmChart.packagedArtifactConfigurationName: String
    get() = namePattern.mapName(name)


/**
 * Gets the name of the [Configuration] that contains the packaged artifact for a chart with the given name.
 *
 * @param chartName the chart name
 * @return the packaged chart artifact configuration name
 */
internal fun chartPackagedArtifactConfigurationName(chartName: String): String =
    namePattern.mapName(chartName)


/**
 * A rule that registers an artifact configuration and an artifact for a chart package.
 *
 * The artifact will contain a single file, which is the tar.gz package file of the chart.
 */
internal class ChartPackagedArtifactRule(
    configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractPatternRule<HelmChart, Configuration>(
    configurations, charts, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun Configuration.configureFrom(chart: HelmChart) {

        isCanBeResolved = false
        isCanBeConsumed = true

        outgoing { publications ->
            publications.artifact(chart.packageFile) { artifact ->
                artifact.builtBy(tasks.named(chart.packageTaskName))
                artifact.name = chart.chartName.get()
                artifact.extension = "tgz"
            }
        }
    }
}
