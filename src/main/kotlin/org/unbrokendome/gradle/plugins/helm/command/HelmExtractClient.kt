package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.dsl.HelmDownloadClient
import org.unbrokendome.gradle.plugins.helm.util.SystemUtils
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.withLockFile


/**
 * Downloads and extracts a Helm client executable package.
 */
abstract class HelmExtractClient : DefaultTask() {

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
            "helm.client.download.group", defaultValue = HelmDownloadClient.DEFAULT_HELM_CLIENT_GROUP
        )


    /**
     * The version of the Helm client to be downloaded.
     */
    @get:Input
    abstract val version: Property<String>


    @get:Input
    internal val osClassifier: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.osclassifier",
            defaultValue = SystemUtils.getOperatingSystemClassifier()
        )


    @get:Internal("Represented as part of executable")
    abstract val baseDestinationDir: DirectoryProperty


    @get:Internal("Represented as part of executable")
    abstract val destinationDir: DirectoryProperty


    /**
     * Path of the extracted Helm client executable.
     */
    @get:OutputFile
    val executable: Provider<RegularFile> =
        destinationDir.file(
            osClassifier.map { osClassifier ->
                val extension = if (osClassifier.startsWith("windows-")) ".exe" else ""
                "$osClassifier/helm${extension}"
            }
        )


    init {
        destinationDir.convention(baseDestinationDir.dir(version))
    }


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
}
