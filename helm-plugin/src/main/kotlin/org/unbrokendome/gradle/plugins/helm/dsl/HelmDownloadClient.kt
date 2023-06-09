package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import com.citi.gradle.plugins.helm.command.rules.extractClientTaskName
import com.citi.gradle.plugins.helm.command.tasks.HelmExtractClient
import org.unbrokendome.gradle.pluginutils.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty
import javax.inject.Inject


/**
 * Configures downloading of the Helm client executable, as an alternative to specifying the path to a
 * local executable.
 */
interface HelmDownloadClient {

    companion object {

        /**
         * Default version of the Helm client executable. This is the latest version available at the time
         * the plugin is released.
         */
        @JvmStatic
        val DEFAULT_HELM_CLIENT_VERSION = "3.7.1"
    }

    /**
     * Whether to download the Helm client. Defaults to `false`.
     *
     * Can be configured using the `helm.client.download.enabled` project property.
     */
    val enabled: Property<Boolean>

    /**
     * The version of the client to be downloaded.
     *
     * Defaults to the latest version available at the time of the plugin release (currently `3.4.1`).
     *
     * @see DEFAULT_HELM_CLIENT_VERSION
     */
    val version: Property<String>
}


internal interface HelmDownloadClientInternal : HelmDownloadClient {

    /**
     * The task that extracts the Helm client executable.
     *
     * If the automatic client download is [enabled], then this will point to a task in the
     * root project for the desired version. If not [enabled], the provider will have no value.
     */
    val extractClientTask: Provider<HelmExtractClient>

    /**
     * Path of the extracted executable file.
     */
    val executable: Provider<RegularFile>
}


internal open class DefaultHelmDownloadClient
@Inject constructor(
    private val project: Project
) : HelmDownloadClient, HelmDownloadClientInternal {

    override val enabled: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(
                project.booleanProviderFromProjectProperty("helm.client.download.enabled", false)
            )


    final override val version: Property<String> =
        project.objects.property<String>()
            .convention(
                project.providerFromProjectProperty(
                    "helm.client.download.version", HelmDownloadClient.DEFAULT_HELM_CLIENT_VERSION
                )
            )


    override val extractClientTask: Provider<HelmExtractClient> =
        version.flatMap { version ->
            if (enabled.get()) {
                project.rootProject.tasks.named(extractClientTaskName(version), HelmExtractClient::class.java)
            } else {
                project.provider { null }
            }
        }


    override val executable: Provider<RegularFile>
        // Need to use flatMap here because map isn't allowed to return null
        get() = enabled.flatMap { enabled ->
            if (enabled) {
                extractClientTask.flatMap { it.executable }
            } else {
                project.provider { null }
            }
        }
}
