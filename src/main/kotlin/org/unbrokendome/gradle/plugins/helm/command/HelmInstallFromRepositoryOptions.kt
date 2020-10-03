package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.slf4j.LoggerFactory
import java.net.URI


interface HelmInstallFromRepositoryOptions : HelmInstallationOptions {

    val repository: Provider<URI>

    val username: Provider<String>

    val password: Provider<String>

    val caFile: Provider<RegularFile>

    val certFile: Provider<RegularFile>

    val keyFile: Provider<RegularFile>
}


interface ConfigurableHelmInstallFromRepositoryOptions
    : ConfigurableHelmInstallationOptions, HelmInstallFromRepositoryOptions {

    /**
     * Chart repository URL where to locate the requested chart.
     *
     * Corresponds to the `--repo` Helm CLI parameter.
     */
    @get:[Input Optional]
    override val repository: Property<URI>


    /**
     * Chart repository username where to locate the requested chart.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:Internal
    override val username: Property<String>


    /**
     * Chart repository password where to locate the requested chart.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    @get:Internal
    override val password: Property<String>


    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:Internal
    override val caFile: RegularFileProperty


    /**
     * Identify HTTPS client using this SSL certificate file.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:Internal
    override val certFile: RegularFileProperty


    /**
     * Identify HTTPS client using this SSL key file.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:Internal
    override val keyFile: RegularFileProperty
}


internal fun ConfigurableHelmInstallFromRepositoryOptions.setFrom(source: HelmInstallFromRepositoryOptions) {
    setFrom(source as HelmInstallationOptions)
    repository.set(source.repository)
    username.set(source.username)
    password.set(source.password)
    caFile.set(source.caFile)
    certFile.set(source.certFile)
    keyFile.set(source.keyFile)
}


internal object HelmInstallFromRepositoryOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmInstallFromRepositoryOptions) {
            with (spec) {

                logger.debug("Applying options: {}", options)

                option("--repo", options.repository)
                option("--username", options.username)
                option("--password", options.password)
                option("--ca-file", options.caFile)
                option("--cert-file", options.certFile)
                option("--key-file", options.keyFile)
            }
        }
    }
}
