package com.citi.gradle.plugins.helm.command

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.citi.gradle.plugins.helm.command.rules.DownloadClientPackageTaskRule
import com.citi.gradle.plugins.helm.command.rules.ExtractClientTaskRule
import org.unbrokendome.gradle.pluginutils.dirProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty
import java.net.URI


/**
 * A plugin that adds task(s) to download and install the Helm client.
 *
 * This plugin is added to the root project so that multiple subprojects using Helm can sync on
 * the Helm client, and it needs to be downloaded only once. It is automatically applied to the
 * root project if any project in the build uses a Helm plugin. It does not need to be added
 * explicitly by the build script.
 */
internal class HelmDownloadClientPlugin : Plugin<Project> {

    companion object {
        @JvmStatic
        val DEFAULT_BASE_URL = URI("https://get.helm.sh/")
    }


    override fun apply(project: Project) {

        val baseUrl = project.providerFromProjectProperty(
            "helm.client.download.baseUrl", defaultValue = DEFAULT_BASE_URL.toString()
        ).map { URI(if (it.endsWith("/")) it else "$it/") }

        val downloadDir = project.dirProviderFromProjectProperty(
            "helm.client.download.dir", defaultPath = ".gradle/helm/client/download"
        )
        val extractDir = project.dirProviderFromProjectProperty(
            "helm.client.extract.dir", defaultPath = ".gradle/helm/client"
        )

        project.tasks.addRule(
            DownloadClientPackageTaskRule(project, baseUrl, downloadDir)
        )
        project.tasks.addRule(
            ExtractClientTaskRule(project, extractDir)
        )
    }
}
