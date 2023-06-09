package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import com.citi.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.pluginutils.SystemUtils
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty


/**
 * Extracts a Helm client executable package.
 */
open class HelmExtractClient : DefaultTask() {

    init {
        group = HELM_GROUP
    }


    /**
     * The version of the Helm client to be downloaded.
     */
    @get:Input
    val version: Property<String> =
        project.objects.property()


    /**
     * The input archive file that was downloaded.
     */
    @get:InputFile
    val archiveFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * The OS classifier. Will use the project property `helm.client.download.osclassifier`
     * if available, or auto-detect the OS otherwise.
     */
    @get:Input
    internal val osClassifier: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.osclassifier",
            defaultValue = SystemUtils.getOperatingSystemClassifier()
        )


    /**
     * The base directory for extracting the contents of Helm client packages.
     *
     * This is used for constructing the default value of [destinationDir]; if [destinationDir]
     * is explicitly set then this is ignored.
     */
    @get:Internal("Represented as part of executable")
    val baseDestinationDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * The directory for extracting the contents of the Helm client package for [version].
     *
     * This defaults to a directory named [version] under the [baseDestinationDir].
     */
    @get:Internal("Represented as part of executable")
    val destinationDir: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(baseDestinationDir.dir(version))


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


    @TaskAction
    fun extractClient() {

        val archiveFile = project.file(archiveFile)

        project.copy { copy ->
            copy.from(
                if (archiveFile.extension == "zip") project.zipTree(archiveFile) else project.tarTree(archiveFile)
            )
            copy.into(destinationDir)
        }
    }
}
