package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.HelmRendering
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmRender<Chart>Chart")


/**
 * The name of the task that renders all renderings for this chart.
 *
 * @receiver the [HelmChart]
 */
internal val HelmChart.renderAllTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that will create a task to render all renderings for a given chart.
 */
internal class RenderAllTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<Task>(
    Task::class.java, tasks, charts, namePattern
) {
    override fun Task.configureFrom(chart: HelmChart) {
        group = HELM_GROUP
        description = "Renders all renderings for the ${chart.name} chart."
        dependsOn(
            TaskDependency {
                val renderingNames = chart.renderings.names + setOf(HelmRendering.DEFAULT_RENDERING_NAME)
                renderingNames.mapTo(mutableSetOf<Task>()) { renderingName ->
                    val taskName = chart.renderTaskName(renderingName)
                    tasks.getByName(taskName)
                }
            }
        )
    }
}
