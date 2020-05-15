package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.util.GRADLE_VERSION_6_2
import org.unbrokendome.gradle.plugins.helm.util.SystemUtils
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.withLockFile


/**
 * Downloads and extracts a Helm client executable package.
 */
open class HelmExtractClient : DefaultTask() {

    private companion object {

        const val DEFAULT_HELM_CLIENT_GROUP = "sh.helm"
    }


    init {
        group = HELM_GROUP
    }


    /**
     * A pseudo "group" coordinate for the Helm client artifacts; it is not used in the actual artifact URL
     * but to make sure that Helm artifacts are only downloaded from the one repository declared here
     * and don't interfere with any other repositories declared in the project.
     */
    @get:Input
    internal val helmGroup: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.group", defaultValue = DEFAULT_HELM_CLIENT_GROUP
        )


    /**
     * The version of the Helm client to be downloaded.
     */
    @get:Input
    val version: Property<String> =
        project.objects.property()


    @get:Input
    internal val osClassifier: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.osclassifier",
            defaultValue = SystemUtils.getOperatingSystemClassifier()
        )


    @get:Internal("Represented as part of executable")
    val baseDestinationDir: DirectoryProperty =
        project.objects.directoryProperty()


    @get:Internal("Represented as part of executable")
    val destinationDir: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(baseDestinationDir.dir(version))


    /**
     * Path of the extracted Helm client executable.
     */
    @get:[OutputFile PathSensitive(PathSensitivity.RELATIVE)]
    val executable: Provider<RegularFile> =
        destinationDir.file(
            osClassifier.map { osClassifier ->
                val extension = if (osClassifier.startsWith("windows-")) ".exe" else ""
                "$osClassifier/helm${extension}"
            }
        )


    @TaskAction
    fun extractClient() {

        val executable = executable.get().asFile

        // If the output file already exists, there is nothing to do
        if (executable.exists()) {
            didWork = false
            return
        }

        // Use a lockfile so we don't have two tasks from different projects extracting the client package at once
        withLockFile(destinationDir.get().asFile.resolve("gradle-helm-extract.lock")) {

            // Check again if the output file exists
            if (executable.exists()) {
                didWork = false
                return@withLockFile
            }

            project.createHelmClientRepository()

            val osClassifier = osClassifier.get()
            val archiveFormat = osClassifier.let {
                if (it.startsWith("windows-")) "zip" else "tar.gz"
            }

            val dependency = project.dependencies.create(
                "${helmGroup.get()}:helm:${version.get()}:${osClassifier}@${archiveFormat}"
            )
            val configuration = project.configurations.detachedConfiguration(dependency)
            val archiveFile = configuration.singleFile

            val from =
                if (archiveFile.extension == "zip") project.zipTree(archiveFile) else project.tarTree(archiveFile)

            project.copy { copy ->
                copy.from(from)
                copy.into(destinationDir)
            }
        }
    }


    private fun Project.createHelmClientRepository() {

        val helmGroup = helmGroup.get()

        val repository = repositories.ivy { repo ->
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
