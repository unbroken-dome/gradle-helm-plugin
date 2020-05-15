package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.unbrokendome.gradle.plugins.helm.command.HelmExtractClient
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Configures downloading of the Helm client executable, as an alternative to specifying the path to a
 * local executable.
 */
interface HelmDownloadClient {

    companion object {

        @JvmStatic
        val HELM_EXTRACT_CLIENT_TASK_NAME = "helmExtractClient"

        /**
         * Default version of the Helm client executable. This is the latest version available at the time
         * the plugin is released.
         */
        @JvmStatic
        val DEFAULT_HELM_CLIENT_VERSION = "3.2.0"
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
     * Defaults to the latest version available at the time of the plugin release (currently `3.2.0`).
     *
     * @see DEFAULT_HELM_CLIENT_VERSION
     */
    val version: Property<String>

    /**
     * The local directory where downloaded Helm client artifacts will be extracted.
     *
     * Defaults to `.gradle/helm/client` in the _root project_ directory, and can also be configured using the
     * `helm.client.download.destinationDir` project property.
     */
    val destinationDir: DirectoryProperty
}


internal interface HelmDownloadClientInternal : HelmDownloadClient {

    /**
     * Path of the extracted executable file.
     */
    val executable: Provider<RegularFile>
}


internal class DefaultHelmDownloadClient
constructor(
    project: Project
) : HelmDownloadClient, HelmDownloadClientInternal {


    override val enabled: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(
                project.booleanProviderFromProjectProperty("helm.client.download.enabled", false)
            )


    override val version: Property<String> =
        project.objects.property<String>()
            .convention(HelmDownloadClient.DEFAULT_HELM_CLIENT_VERSION)


    override val destinationDir: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(
                project.rootProject.layout.projectDirectory.dir(".gradle/helm/client")
            )


    private val extractClientTask: TaskProvider<HelmExtractClient> =
        project.tasks.register(
            HelmDownloadClient.HELM_EXTRACT_CLIENT_TASK_NAME,
            HelmExtractClient::class.java
        ) { task ->
            task.onlyIf { enabled.get() }
            task.version.set(version)
            task.baseDestinationDir.set(destinationDir)
        }


    override val executable: Provider<RegularFile>
        get() = extractClientTask.flatMap { it.executable }
}
