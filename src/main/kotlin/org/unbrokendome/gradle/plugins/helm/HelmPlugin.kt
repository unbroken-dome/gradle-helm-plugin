package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.dsl.helmChartContainer


class HelmPlugin
    : Plugin<Project> {

    internal companion object {
        const val initClientTaskName = "helmInitClient"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        createChartsExtension(project)
        createFilteringExtension(project)

        project.tasks.create(initClientTaskName, HelmInit::class.java) { task ->
            task.clientOnly.set(true)
        }
    }
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
