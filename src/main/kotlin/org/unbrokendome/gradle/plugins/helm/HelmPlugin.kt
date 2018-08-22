package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInit


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
    }
}
