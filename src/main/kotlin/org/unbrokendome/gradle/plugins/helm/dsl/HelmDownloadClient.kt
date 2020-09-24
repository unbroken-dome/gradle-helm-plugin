package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.unbrokendome.gradle.plugins.helm.command.HelmExtractClient
import org.unbrokendome.gradle.plugins.helm.util.GRADLE_VERSION_6_2
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import javax.inject.Inject


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


        internal const val DEFAULT_HELM_CLIENT_GROUP = "sh.helm"
        internal const val REPOSITORY_NAME = "_helmClientReleases"
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


internal abstract class DefaultHelmDownloadClient
@Inject constructor(
    private val project: Project
) : HelmDownloadClient, HelmDownloadClientInternal {

    private val extractClientTask: TaskProvider<HelmExtractClient> =
        project.tasks.register(
            HelmDownloadClient.HELM_EXTRACT_CLIENT_TASK_NAME,
            HelmExtractClient::class.java
        ) { task ->
            task.onlyIf { enabled.getOrElse(false) }
            task.version.set(version)
            task.baseDestinationDir.set(destinationDir)
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

    /**
     * A pseudo "group" coordinate for the Helm client artifacts; it is not used in the actual artifact URL
     * but to make sure that Helm artifacts are only downloaded from the one repository declared here
     * and don't interfere with any other repositories declared in the project.
     */
    private val helmGroup: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.group", defaultValue = HelmDownloadClient.DEFAULT_HELM_CLIENT_GROUP
        )


    init {
        applyConventions()
        project.createHelmClientRepository()
    }


    private fun applyConventions() {
        enabled.convention(
            project.booleanProviderFromProjectProperty("helm.client.download.enabled", false)
        )
        version.convention(
            project.providerFromProjectProperty(
                "helm.client.download.version", HelmDownloadClient.DEFAULT_HELM_CLIENT_VERSION
            )
        )
        destinationDir.convention(
            project.rootProject.layout.projectDirectory.dir(".gradle/helm/client")
        )
    }


    private fun Project.createHelmClientRepository() {

        if (repositories.findByName(HelmDownloadClient.REPOSITORY_NAME) != null) {
            return
        }

        val helmGroup = helmGroup.get()

        val repository = repositories.ivy { repo ->
            repo.name = HelmDownloadClient.REPOSITORY_NAME
            repo.url = uri(findProperty("helm.client.download.baseUrl") ?: "https://get.helm.sh")
            repo.patternLayout { layout ->
                layout.artifact("[module]-v[revision]-[classifier].[ext]")
            }
            repo.metadataSources { sources ->
                sources.artifact()
            }
        }

        if (GradleVersion.current() >= GRADLE_VERSION_6_2) {
            repositories.exclusiveContent {
                it.filter { filter -> filter.includeGroup(helmGroup) }
                it.forRepositories(repository)
            }

        } else {
            // Before Gradle 6.2, we don't have exclusiveContent, so we need to
            // (a) specify that our repository hosts the Helm client artifacts, and
            // (b) specify that all other repositories don't have them
            repository.content { content ->
                content.includeGroup(helmGroup)
            }
            repositories.all { otherRepo ->
                if (otherRepo != repository) {
                    otherRepo.content { content ->
                        content.excludeGroup(helmGroup)
                    }
                }
            }
        }
    }
}
