package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage


/**
 * A rule that creates a [HelmPackage] task for each chart.
 */
internal class PackageTaskRule(
        private val tasks: TaskContainer,
        private val charts: Iterable<HelmChart>)
    : AbstractRule() {

    internal companion object {
        fun getTaskName(chartName: String) =
                "helmPackage${chartName.capitalize()}Chart"
    }

    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
            "Pattern: ${getTaskName("<Chart>")}"


    override fun apply(taskName: String) {
        if (regex.matches(taskName)) {
            charts.find { it.packageTaskName == taskName }
                    ?.let { chart ->

                        val filterSourcesTask = tasks.getByName(chart.filterSourcesTaskName) as HelmFilterSources

                        tasks.create(taskName, HelmPackage::class.java) { task ->
                            task.description = "Packages the ${chart.name} chart."

                            task.dependsOn(filterSourcesTask, chart.lintTaskName)
                            task.chartName.set(chart.chartName)
                            task.chartVersion.set(chart.chartVersion)
                            task.sourceDir.set(filterSourcesTask.targetDir)
                            task.saveToLocalRepo.set(false)
                            task.updateDependencies.set(false)
                            task.chartOutputPath
                        }
                    }
        }
    }
}


/**
 * The name of the [HelmPackage] task for this chart.
 */
val HelmChart.packageTaskName
    get() = PackageTaskRule.getTaskName(name)
