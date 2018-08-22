package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property
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
     * Username to access the chart repository.
     */
    @get:[Input Optional]
    val username: Property<String> =
            project.objects.property()


    /**
     * Password to access the chart repository.
     */
    @get:[Input Optional]
    val password: Property<String> =
            project.objects.property()


    /**
     * If set to `true`, fails if the repository is already registered.
     *
     * Corresponds to the `--no-update` command line flag.
     */
    @get:Internal
    val failIfExists: Property<Boolean> =
            project.objects.property()


    init {
        @Suppress("LeakingThis")
        inputs.dir(home)
    }


    @TaskAction
    fun addRepository() {
        execHelm("repo", "add") {
            option("--username", username)
            option("--password", password)
            flag("--no-update", failIfExists)
            args(repositoryName)
            args(url)
        }
    }
}
