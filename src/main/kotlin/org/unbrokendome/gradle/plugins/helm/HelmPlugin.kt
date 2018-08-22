package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.helm


class HelmPlugin
    : Plugin<Project> {

    internal companion object {
        const val initClientTaskName = "helmInitClient"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        project.tasks.create(initClientTaskName, HelmInit::class.java) { task ->
            task.clientOnly.set(true)
        }

        (project.helm as ExtensionAware).extensions
                .add(Filtering::class.java, HELM_FILTERING_EXTENSION_NAME, createFiltering(project.objects))
    }
}
