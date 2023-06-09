package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import com.citi.gradle.plugins.helm.command.HelmDownloadClientPlugin
import com.citi.gradle.plugins.helm.util.calculateDigestHex
import com.citi.gradle.plugins.helm.util.formatDataSize
import org.unbrokendome.gradle.pluginutils.SystemUtils
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.net.URI


/**
 * Downloads a Helm client package from the official Helm releases website.
 */
abstract class HelmDownloadClientPackage : DefaultTask() {

    companion object {

        private const val BufferSize = 10 * 1024
        private const val ConnectTimeoutMilliseconds = 30 * 1000
        private const val ReadTimeoutMilliseconds = 30 * 1000
        private const val ProgressChunkSize = 1024 * 256
    }


    /**
     * The version of the Helm client to be downloaded.
     */
    @get:Input
    val version: Property<String> =
        project.objects.property()


    /**
     * The base URL. Defaults to `https://get.helm.sh`.
     */
    @get:Internal("Represented as part of downloadUrl")
    val baseUrl: Property<URI> =
        project.objects.property<URI>()
            .convention(HelmDownloadClientPlugin.DEFAULT_BASE_URL)


    /**
     * The OS classifier. Will use the project property `helm.client.download.osclassifier`
     * if available, or auto-detect the OS otherwise.
     */
    @get:Internal("Represented as part of downloadUrl")
    internal val osClassifier: Provider<String> =
        project.providerFromProjectProperty(
            "helm.client.download.osclassifier",
            defaultValue = SystemUtils.getOperatingSystemClassifier()
        )


    /**
     * The archive format / file extension (without a leading dot), this will be `zip`
     * for Windows client packages and `tar.gz` for Linux and MacOS.
     */
    @get:Internal("Represented as part of downloadUrl")
    internal val archiveFormat: Provider<String> =
        osClassifier.map {
            if (it.startsWith("windows-")) "zip" else "tar.gz"
        }


    /**
     * The name of the package file.
     *
     * This is constructed according to the format `helm-v<version>-<osClassifier>.<archiveFormat>`,
     * e.g. `helm-v3.5.0-darwin-amd64.tar.gz`
     */
    @get:Internal("Represented as part of downloadUrl and outputFile")
    internal val packageFileName: Provider<String> =
        version.map { version ->
            "helm-v${version}-${osClassifier.get()}.${archiveFormat.get()}"
        }


    /**
     * The name of the sha256sum file containing the digest of the package file.
     *
     * This is constructed by appending the extension `.sha256sum` to the [packageFileName].
     */
    @get:Internal("Represented as part of sha256SumUrl and sha256SumFile")
    internal val sha256SumFileName: Provider<String> =
        packageFileName.map { "$it.sha256sum" }


    /**
     * The full URL from which the client package that will be downloaded.
     */
    @get:Input
    val downloadUrl: Provider<URI> =
        project.provider {
            val baseUrl = this.baseUrl.orNull
            val packageFileName = this.packageFileName.orNull
            if (baseUrl != null && packageFileName != null) {
                baseUrl.resolve(packageFileName)
            } else null
        }


    /**
     * The URL from which a text file containing the SHA-256 checksum can be downloaded.
     */
    @get:Input
    val sha256SumUrl: Provider<URI> =
        project.provider {
            val baseUrl = this.baseUrl.orNull
            val sha256SumFileName = sha256SumFileName.orNull
            if (baseUrl != null && sha256SumFileName != null) {
                baseUrl.resolve(sha256SumFileName)
            } else null
        }


    /**
     * The directory in the local filesystem where the downloaded client package will be placed.
     *
     * A separate directory will be created beneath this base directory for each downloaded version.
     */
    @get:Internal("Represented as part of outputFile and sha256SumFile")
    val destinationDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * The path to a file in the local filesystem where the downloaded client package will be placed.
     */
    @get:OutputFile
    val outputFile: Provider<RegularFile> =
        destinationDir.file(packageFileName)


    /**
     * The path to a file in the local filesystem where the sha256sum file for the client package will be placed.
     */
    @get:OutputFile
    val sha256SumFile: Provider<RegularFile> =
        destinationDir.file(sha256SumFileName)


    @TaskAction
    fun downloadClientPackage() {

        val sha256SumFile = project.file(sha256SumFile)
        val targetFile = project.file(outputFile)

        sha256SumFile.delete()
        targetFile.delete()

        downloadSha256SumFile(sha256SumFile)

        try {
            val expectedDigest = readSha256DigestFromFile()

            // If the target file already exists and matches then we don't need to download it again
            if (verifySha256Digest(expectedDigest, project.file(outputFile))) {
                return
            }

            downloadClientPackageFile(targetFile)

            if (!verifySha256Digest(expectedDigest, targetFile)) {
                throw IllegalStateException("SHA-256 digest mismatch on downloaded file $targetFile")
            }

        } catch (ex: Exception) {
            logger.info("Deleting already-downloaded sha256sum file at {}", sha256SumFile)
            sha256SumFile.delete()

            if (targetFile.exists()) {
                logger.info("Deleting already-downloaded release package file at {}", targetFile)
                targetFile.delete()
            }

            throw ex
        }
    }


    private fun downloadSha256SumFile(sha256SumFile: File) {
        val sha256SumUrl = sha256SumUrl.get()
        logger.info("Downloading sha256 sum file for Helm client v{} from {}", version.get(), sha256SumUrl)
        downloadFile(sha256SumUrl, sha256SumFile)
    }


    private fun downloadClientPackageFile(targetFile: File) {
        val downloadUrl = downloadUrl.get()
        logger.info("Downloading release package for Helm client v{} from {}", version.get(), downloadUrl)
        downloadFile(downloadUrl, targetFile)
    }


    /**
     * Calculates a SHA-256 digest over [targetFile] and compares it with an expected digest.
     *
     * @param expectedDigest the expected SHA-256 digest as a hex-encoded string
     * @param targetFile the file to be verified
     * @return `true` if the digest matches
     */
    private fun verifySha256Digest(expectedDigest: String, targetFile: File): Boolean {
        if (!targetFile.exists()) return false

        logger.info("Calculating digest for file {}", targetFile)
        val actualDigest = targetFile.calculateDigestHex("SHA-256")
        return expectedDigest.equals(actualDigest, ignoreCase = true)
    }


    /**
     * Downloads a file from the given URL.
     *
     * @param url the URL from which to download the file
     * @param targetFile the local path to store the downloaded file at
     */
    private fun downloadFile(url: URI, targetFile: File) {

        val connection = url.toURL().openConnection().apply {
            connectTimeout = ConnectTimeoutMilliseconds
            readTimeout = ReadTimeoutMilliseconds
            connect()
        }

        val contentLength = connection.contentLengthLong

        targetFile.parentFile.mkdirs()

        BufferedOutputStream(targetFile.outputStream()).use { output ->

            val bufferSize = minOf(contentLength, BufferSize.toLong()).toInt()
            val buffer = ByteArray(bufferSize)

            var totalBytesTransferred = 0L
            var nextProgressThreshold = 0L

            connection.getInputStream().use { input ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (Thread.currentThread().isInterrupted) {
                        throw IOException("Download was interrupted")
                    }
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)

                    if (logger.isInfoEnabled) {
                        totalBytesTransferred += bytesRead
                        if (totalBytesTransferred >= nextProgressThreshold) {
                            nextProgressThreshold = minOf(contentLength, nextProgressThreshold + ProgressChunkSize)

                            val progress = ((totalBytesTransferred * 1000) / contentLength).toDouble() / 10.0
                            logger.info(
                                "Downloaded {} of {} ({})",
                                formatDataSize(totalBytesTransferred),
                                formatDataSize(contentLength),
                                String.format("%.1f", progress)
                            )
                        }
                    }
                }
            }
        }
    }


    /**
     * Reads the expected SHA-256 digest from the sha256sum file.
     *
     * @return the SHA-256 digest as a hex-encoded string
     */
    private fun readSha256DigestFromFile(): String {
        val file = project.file(sha256SumFile)
        val targetFileName = packageFileName.get()
        val text = file.readText()
        val items = text.trim().split(Regex("\\s+"))
        require(items.size == 2 && items[0].length == 64) { "Invalid contents of sha256sum file $file" }
        require(items[1] == targetFileName) {
            "sha256sum file $file does not describe target file $targetFileName but a different file ${items[1]}"
        }
        return items[0]
    }
}
