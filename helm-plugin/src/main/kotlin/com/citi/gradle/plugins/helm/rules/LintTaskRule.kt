package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.gradle.kotlin.dsl.lint
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.command.tasks.HelmLint
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmLint<Chart>Chart")


/**
 * The name of the task that performs all linting for this chart.
 */
val HelmChart.lintTaskName
    get() = namePattern.mapName(name)


/**
 * A rule that creates a lint task for each chart.
 *
 * - If the chart does not have lint configurations, creates task is of type [HelmLint]
 * - If the chart has lint configurations, creates a simple [Task] that depends on all configuration-specific
 *   [HelmLint] tasks.
 */
internal class LintTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<Task>(Task::class.java, tasks, charts, namePattern) {

    override fun Task.configureFrom(chart: HelmChart) {

        description = "Lints the ${chart.name} chart."
        group = HELM_GROUP

        val chartLint = chart.lint

        onlyIf { chartLint.enabled.get() }

        // poke the "default" configuration so it is created if we don't have any configurations
        chartLint.configurations.findByName(Linting.Configuration.DEFAULT_CONFIGURATION_NAME)

        dependsOn(TaskDependency {
            chartLint.configurations.map { lintConfiguration ->
                tasks.getByName(chart.lintTaskName(lintConfiguration.name))
            }.toSet()
        })
    }
}
