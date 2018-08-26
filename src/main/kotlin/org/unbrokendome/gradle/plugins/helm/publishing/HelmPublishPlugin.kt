package org.unbrokendome.gradle.plugins.helm.publishing

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.charts
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.*
import org.unbrokendome.gradle.plugins.helm.publishing.rules.HelmPublishChartTaskRule
import org.unbrokendome.gradle.plugins.helm.publishing.rules.HelmPublishChartToRepositoryTaskRule
import org.unbrokendome.gradle.plugins.helm.publishing.rules.publishTaskName


/**
 * A Gradle plugin that adds publishing capabilities for Helm charts.
 */
class HelmPublishPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        val publishingExtension = createPublishingExtension(project)

        project.plugins.withType(HelmPlugin::class.java) { _ ->

            val charts = project.helm.charts
            charts.all { chart ->
                addChartPublishConvention(chart, project)
            }

            project.tasks.run {
                val repositories = publishingExtension.repositories
                addRule(HelmPublishChartToRepositoryTaskRule(project, charts, repositories))
                addRule(HelmPublishChartTaskRule(project, charts, repositories))
            }

            createPublishAllTask(project, charts)
        }
    }


    /**
     * Creates and registers a `helm.publishing` sub-extension with the DSL.
     */
    private fun createPublishingExtension(project: Project) =
            createHelmPublishingExtension(project)
                    .apply {
                        (project.helm as ExtensionAware).extensions.add(
                                HelmPublishingExtension::class.java,
                                HELM_PUBLISHING_EXTENSION_NAME,
                                this)
                    }


    /**
     * Adds a convention object to a chart for holding publishing-related properties.
     *
     * @see HelmChartPublishConvention
     */
    private fun addChartPublishConvention(chart: HelmChart, project: Project) {
        (chart as HasConvention).convention.add(HelmChartPublishConvention::class.java,
                HELM_CHART_PUBLISHING_CONVENTION_NAME,
                createHelmChartPublishConvention(project.objects))
    }


    /**
     * Create a task that publishes all charts in the project to all publishing repositories.
     */
    private fun createPublishAllTask(project: Project, charts: NamedDomainObjectContainer<HelmChart>) {
        project.tasks.create("helmPublish") { task ->
            task.group = HELM_GROUP
            task.description = "Publishes all Helm charts."

            task.dependsOn(TaskDependency {
                charts.map { chart -> project.tasks.getByName(chart.publishTaskName) }
                        .toSet()
            })
        }
    }
}


