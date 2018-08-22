package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin


class HelmPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(HelmCommandsPlugin::class.java)
    }
}
