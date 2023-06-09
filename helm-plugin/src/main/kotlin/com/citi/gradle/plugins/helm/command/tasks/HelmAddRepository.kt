package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.pluginutils.property
import org.yaml.snakeyaml.Yaml
import java.net.URI


/**
 * Registers a known repository with Helm. Corresponds to the `helm repo add` CLI command.
 */
open class HelmAddRepository : AbstractHelmCommandTask() {

    /**
     * Name of the repository.
     */
    @get:Input
    val repositoryName: Property<String> =
        project.objects.property()


    /**
     * URL of the repository.
     */
    @get:Input
    val url: Property<URI> =
        project.objects.property()


    /**
     * A CA bundle used to verify certificates of HTTPS-enabled servers.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:[InputFile Optional]
    val caFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Username to access the chart repository.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:[Input Optional]
    val username: Property<String> =
        project.objects.property()


    /**
     * Password to access the chart repository.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    @get:[Input Optional]
    val password: Property<String> =
        project.objects.property()


    /**
     * Path to a certificate file for client SSL authentication.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:[InputFile Optional]
    val certificateFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Path to a certificate private key file for client SSL authentication.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:[InputFile Optional]
    val keyFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * If set to `true`, fails if the repository is already registered.
     *
     * Corresponds to the `--no-update` command line flag.
     */
    @get:Internal
    val failIfExists: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun addRepository() {
        execHelm("repo", "add") {
            option("--ca-file", caFile)
            option("--cert-file", certificateFile)
            option("--key-file", keyFile)
            option("--username", username)
            option("--password", password)
            flag("--no-update", failIfExists)
            args(repositoryName)
            args(url)
        }
    }


    init {
        outputs.file(repositoryConfigFile)
            .withPropertyName("repositoryConfigFile")
            .optional()
        outputs.upToDateWhen { task -> checkUpToDate(task) }
    }


    private fun checkUpToDate(task: Task): Boolean {

        // If we should fail if the repo exists, let Helm handle it
        if (failIfExists.getOrElse(false)) {
            logger.debug("{} is not up-to-date because the \"failIfExists\" flag is set.", task)
            return false
        }

        val actualConfig = loadRepositoryConfig()
        if (actualConfig == null) {
            logger.debug("{} is not up-to-date because the desired repository configuration does not exist.", task)
            return false
        }

        val expectedConfig = RepositoryConfig(
            name = repositoryName.getOrElse(""),
            url = url.map { it.toString() }.getOrElse(""),
            username = username.getOrElse(""),
            password = password.getOrElse(""),
            caFile = caFile.map { it.asFile.absolutePath }.getOrElse(""),
            certFile = certificateFile.map { it.asFile.absolutePath }.getOrElse(""),
            keyFile = keyFile.map { it.asFile.absolutePath }.getOrElse("")
        )

        return if (actualConfig == expectedConfig) {
            logger.debug(
                "{} is up-to-date because the current repository configuration matches the desired one.", task
            )
            true
        } else {
            logger.debug(
                "{} is not up-to-date because the current repository configuration does not match the desired one.",
                task
            )
            false
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun loadRepositoryConfig(): RepositoryConfig? {

        val repositoryName = this.repositoryName.get()

        return project.file(this.repositoryConfigFile)
            .takeIf { it.exists() }
            ?.run {
                runCatching {
                    inputStream().use { input ->
                        val map = Yaml().loadAs(input, Map::class.java)
                        val repositories = map["repositories"] as List<Map<String, String>>
                        repositories.asSequence()
                            .filter { it["name"] == repositoryName }
                            .map { RepositoryConfig.fromMap(it) }
                            .firstOrNull()
                    }
                }.getOrNull()
            }
    }


    private data class RepositoryConfig(
        val name: String,
        val url: String,
        val username: String,
        val password: String,
        val caFile: String,
        val certFile: String,
        val keyFile: String
    ) {

        companion object {

            fun fromMap(map: Map<String, String?>) = RepositoryConfig(
                name = map["name"] ?: "",
                url = map["url"] ?: "",
                username = map["username"] ?: "",
                password = map["password"] ?: "",
                caFile = map["caFile"] ?: "",
                certFile = map["certFile"] ?: "",
                keyFile = map["keyFile"] ?: ""
            )
        }
    }
}
