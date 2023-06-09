package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.lint
import com.citi.gradle.plugins.helm.command.internal.mergeValues
import com.citi.gradle.plugins.helm.command.tasks.HelmLint
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


private val namePattern =
    RuleNamePattern2.parse("helmLint<Chart>Chart<Configuration>")



/**
 * The name of the [HelmLint] task for this chart.
 *
 * @param lintConfigurationName the name of the linter configuration
 */
fun HelmChart.lintTaskName(lintConfigurationName: String) =
    namePattern.mapName(name, lintConfigurationName)


internal class LintWithConfigurationTaskRule(
    tasks: TaskContainer, charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRuleOuterInner<Linting.Configuration, HelmLint>(
    HelmLint::class.java, tasks, charts, { chart -> chart.lint.configurations }, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun HelmLint.configureFrom(chart: HelmChart, lintConfiguration: Linting.Configuration) {

        description = "Lints the ${chart.name} chart using the \"${lintConfiguration.name}\" configuration."

        chartDir.set(chart.outputDir)

        chart.lint.let { chartLint ->
            onlyIf { chartLint.enabled.get() }
            strict.set(chartLint.strict)
            withSubcharts.set(chartLint.withSubcharts)
            mergeValues(chartLint)
        }
        mergeValues(lintConfiguration)

        dependsOn(
            chart.updateDependenciesTaskName
        )
    }
}
