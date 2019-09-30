package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property
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
    @get:[Input Optional]
    val caFile: Property<RegularFile> =
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
    @get:[Input Optional]
    val certificateFile: Property<RegularFile> =
        project.objects.fileProperty()


    /**
     * Path to a certificate private key file for client SSL authentication.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:[Input Optional]
    val keyFile: Property<RegularFile> =
        project.objects.fileProperty()


    /**
     * If set to `true`, fails if the repository is already registered.
     *
     * Corresponds to the `--no-update` command line flag.
     */
    @get:Internal
    val failIfExists: Property<Boolean> =
        project.objects.property()


    init {
        outputs.upToDateWhen { checkUpToDate() }
    }


    @TaskAction
    fun addRepository() {
        execHelm("repo", "add") {
            option("--ca-file", caFile)
            option("--username", username)
            option("--password", password)
            flag("--no-update", failIfExists)
            args(repositoryName)
            args(url)
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun checkUpToDate(): Boolean {
        val repositoriesYaml = home.get()
            .file("repository/repositories.yaml")
            .asFile
            .reader().use {
                Yaml().load(it) as Map<String, Any>
            }
        val name = repositoryName.get()
        val repositoryData = (repositoriesYaml["repositories"] as List<Map<String, String>>)
            .find { it["name"] == name }
            ?: return false

        return repositoryData["url"] == url.get().toString() &&
                repositoryData["caFile"] == caFile.orNull?.toString().orEmpty() &&
                repositoryData["username"] == username.orNull.orEmpty() &&
                repositoryData["password"] == password.orNull.orEmpty() &&
                repositoryData["certFile"] == certificateFile.orNull?.toString().orEmpty() &&
                repositoryData["keyFile"] == keyFile.orNull?.toString().orEmpty()
    }
}
