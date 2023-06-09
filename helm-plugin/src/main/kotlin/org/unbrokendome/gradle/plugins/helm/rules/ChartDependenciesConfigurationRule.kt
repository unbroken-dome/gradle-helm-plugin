package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.dependencies.chartDependenciesConfigurationName
import org.unbrokendome.gradle.pluginutils.rules.AbstractPatternRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helm<Chart>Dependencies")


/**
 * Gets the name of the [Configuration] that contains the chart dependencies for this chart.
 *
 * @receiver the [HelmChart]
 * @return the name of the chart's dependencies configuration
 */
internal val HelmChart.dependenciesConfigurationName: String
    get() = chartDependenciesConfigurationName(name)


/**
 * Rule that creates a [Configuration] to hold the dependencies of a [HelmChart].
 */
internal class ChartDependenciesConfigurationRule(
    configurations: ConfigurationContainer,
    charts: NamedDomainObjectCollection<HelmChart>

) : AbstractPatternRule<HelmChart, Configuration>(
    configurations, charts, namePattern
) {

    override fun Configuration.configureFrom(source: HelmChart) {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}
