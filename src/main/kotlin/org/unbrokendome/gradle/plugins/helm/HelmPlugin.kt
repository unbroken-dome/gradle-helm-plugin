package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit
import org.unbrokendome.gradle.plugins.helm.dsl.*


class HelmPlugin
    : Plugin<Project> {

    internal companion object {
        const val initClientTaskName = "helmInitClient"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        createRepositoriesExtension(project)
        createFilteringExtension(project)

        val charts = createChartsExtension(project)
        charts.all { chart ->
            chart.createExtensions(project)
        }

        project.tasks.create(initClientTaskName, HelmInit::class.java) { task ->
            task.clientOnly.set(true)
        }
    }
}


/**
 * Creates and installs the `helm.repositories` sub-extension.
 */
private fun createRepositoriesExtension(project: Project) =
        helmRepositoryContainer(project)
                .apply {
                    (project.helm as ExtensionAware)
                            .extensions.add(HELM_REPOSITORIES_EXTENSION_NAME, this)
                }


/**
 * Creates and installs the `helm.charts` sub-extension.
 */
private fun createChartsExtension(project: Project) =
        helmChartContainer(project)
                .apply {
                    (project.helm as ExtensionAware)
                            .extensions.add(HELM_CHARTS_EXTENSION_NAME, this)
                }


private fun createFilteringExtension(project: Project) =
        createFiltering(project.objects)
                .apply {
                    (project.helm as ExtensionAware)
                            .extensions.add(Filtering::class.java, HELM_FILTERING_EXTENSION_NAME, this)
                }


private fun HelmChart.createExtensions(project: Project) {
    createFilteringExtension(project.objects, project.helm)
    createLintingExtension(project.objects, project.helm)
}


private fun HelmChart.createFilteringExtension(objectFactory: ObjectFactory, helmExtension: HelmExtension) {
    (this as ExtensionAware).extensions
            .add(Filtering::class.java,
                    "filtering",
                    createFiltering(objectFactory, parent = helmExtension.filtering))
}


private fun HelmChart.createLintingExtension(objectFactory: ObjectFactory, helmExtension: HelmExtension) {
    (this as ExtensionAware).extensions
            .add(Linting::class.java,
                    "lint",
                    createLinting(objectFactory, parent = helmExtension.lint))
}