package org.unbrokendome.gradle.plugins.helm.publishing

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.reflect.Instantiator
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.charts
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmChartPublishConvention
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.createHelmChartPublishConvention
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.createHelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.repositories
import org.unbrokendome.gradle.plugins.helm.publishing.rules.HelmPublishChartTaskRule
import org.unbrokendome.gradle.plugins.helm.publishing.rules.HelmPublishChartToRepositoryTaskRule
import org.unbrokendome.gradle.plugins.helm.publishing.rules.publishTaskName
import javax.inject.Inject


/**
 * A Gradle plugin that adds publishing capabilities for Helm charts.
 */
class HelmPublishPlugin
@Inject constructor(
    private val instantiator: Instantiator
) : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        val publishingExtension = createPublishingExtension(project)

        project.plugins.withType(HelmPlugin::class.java) {

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
        project.objects.createHelmPublishingExtension(instantiator)
            .apply {
                (project.helm as ExtensionAware).extensions.add(
                    HelmPublishingExtension::class.java,
                    HELM_PUBLISHING_EXTENSION_NAME,
                    this
                )
            }


    /**
     * Adds a convention object to a chart for holding publishing-related properties.
     *
     * @see HelmChartPublishConvention
     */
    private fun addChartPublishConvention(chart: HelmChart, project: Project) {
        (chart as HasConvention).convention.plugins[HELM_CHART_PUBLISHING_CONVENTION_NAME] =
            project.objects.createHelmChartPublishConvention()
    }


    /**
     * Create a task that publishes all charts in the project to all publishing repositories.
     */
    private fun createPublishAllTask(project: Project, charts: NamedDomainObjectContainer<HelmChart>) {
        project.tasks.register("helmPublish") { task ->
            task.group = HELM_GROUP
            task.description = "Publishes all Helm charts."

            task.dependsOn(TaskDependency {
                charts.map { chart -> project.tasks.getByName(chart.publishTaskName) }
                    .toSet()
            })
        }
    }
}
