package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.dsl.FilteringInternal
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.filtering
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources


/**
 * A rule that creates a [HelmFilterSources] task for each chart.
 */
internal class FilterSourcesTaskRule(
        private val tasks: TaskContainer,
        private val charts: Iterable<HelmChart>)
    : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String) =
                "helmFilter${chartName.capitalize()}ChartSources"
    }


    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
            "Pattern: ${getTaskName("<Chart>")}"


    override fun apply(taskName: String) {
        if (regex.matches(taskName)) {
            charts.find { it.filterSourcesTaskName == taskName }
                    ?.let { chart ->
                        tasks.create(taskName, HelmFilterSources::class.java) { task ->
                            task.description = "Filters the sources for the ${chart.name} chart."
                            task.chartName.set(chart.chartName)
                            task.chartVersion.set(chart.chartVersion)
                            task.sourceDir.set(chart.sourceDir)

                            (task.filtering as FilteringInternal).setParent(chart.filtering)
                        }
                    }
        }
    }
}


/**
 * Gets the name of the [HelmFilterSources] task for this chart.
 */
val HelmChart.filterSourcesTaskName
    get() = FilterSourcesTaskRule.getTaskName(name)
