package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmLint
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.lint
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources


/**
 * A rule that creates a [HelmLint] task for each chart.
 */
internal class LintTaskRule(
    private val tasks: TaskContainer,
    private val charts: Iterable<HelmChart>
) : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String) =
            "helmLint${chartName.capitalize()}Chart"
    }

    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
        "Pattern: ${getTaskName("<Chart>")}"


    override fun apply(taskName: String) {
        if (regex.matches(taskName)) {
            charts.find { it.lintTaskName == taskName }
                ?.let { chart ->

                    val filterSourcesTask = tasks.getByName(chart.filterSourcesTaskName) as HelmFilterSources

                    tasks.create(taskName, HelmLint::class.java) { task ->
                        task.description = "Lints the ${chart.name} chart."
                        task.dependsOn(HelmPlugin.initClientTaskName)

                        task.chartDir.set(filterSourcesTask.targetDir)

                        chart.lint.let { chartLint ->
                            task.onlyIf { chartLint.enabled.get() }
                            task.strict.set(chartLint.strict)
                            task.values.putAll(chartLint.values)
                            task.valueFiles.from(chartLint.valueFiles)
                        }

                        task.dependsOn(chart.updateDependenciesTaskName)
                    }
                }
        }
    }
}


/**
 * The name of the [HelmLint] task for this chart.
 */
val HelmChart.lintTaskName
    get() = LintTaskRule.getTaskName(name)
