package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildDependencies


/**
 * A rule that creates a [HelmBuildDependencies] task for each chart.
 */
internal class BuildDependenciesTaskRule(
        private val tasks: TaskContainer,
        private val charts: Iterable<HelmChart>)
    : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String) =
                "helmBuild${chartName.capitalize()}ChartDependencies"
    }


    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
            "Pattern: " + getTaskName("<Chart>")


    override fun apply(taskName: String) {
        if (regex.matches(taskName)) {
            charts.find { it.buildDependenciesTaskName == taskName }
                    ?.let { chart ->

                        val filterSourcesTask = tasks.getByName(chart.filterSourcesTaskName) as HelmFilterSources

                        tasks.create(taskName, HelmBuildDependencies::class.java) { task ->
                            task.description = "Builds the dependencies for the ${chart.name} chart."
                            task.chartDir.set(filterSourcesTask.targetDir)
                            task.dependsOn(HelmPlugin.initClientTaskName)
                            task.dependsOn(filterSourcesTask)
                        }
                    }
        }
    }
}


/**
 * The name of the [HelmBuildDependencies] task for this chart.
 */
val HelmChart.buildDependenciesTaskName
    get() = BuildDependenciesTaskRule.getTaskName(name)
