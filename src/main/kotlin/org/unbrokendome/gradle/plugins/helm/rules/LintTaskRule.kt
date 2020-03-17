package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.lint
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmLint
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


private val namePattern =
    RuleNamePattern.parse("helmLint<Chart>Chart")


/**
 * The name of the [HelmLint] task for this chart.
 */
val HelmChart.lintTaskName
    get() = namePattern.mapName(name)


/**
 * A rule that creates a [HelmLint] task for each chart.
 */
internal class LintTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<HelmLint>(
    HelmLint::class.java, tasks, charts, namePattern
) {

    override fun HelmLint.configureFrom(chart: HelmChart) {

        description = "Lints the ${chart.name} chart."

        chartDir.set(chart.outputDir)

        chart.lint.let { chartLint ->
            onlyIf { chartLint.enabled.get() }
            strict.set(chartLint.strict)
            values.putAll(chartLint.values)
            fileValues.putAll(chartLint.fileValues)
            valueFiles.from(chartLint.valueFiles)
        }

        dependsOn(
            chart.updateDependenciesTaskName
        )
    }
}
