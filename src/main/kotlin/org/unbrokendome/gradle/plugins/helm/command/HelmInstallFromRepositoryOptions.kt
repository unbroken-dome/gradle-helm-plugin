package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.pluginutils.property
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
    override val repository: Property<URI>


    /**
     * Chart repository username where to locate the requested chart.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    override val username: Property<String>


    /**
     * Chart repository password where to locate the requested chart.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    override val password: Property<String>


    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    override val caFile: RegularFileProperty


    /**
     * Identify HTTPS client using this SSL certificate file.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    override val certFile: RegularFileProperty


    /**
     * Identify HTTPS client using this SSL key file.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
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


internal data class HelmInstallFromRepositoryOptionsHolder(
    private val installationOptions: ConfigurableHelmInstallationOptions,
    override val repository: Property<URI>,
    override val username: Property<String>,
    override val password: Property<String>,
    override val caFile: RegularFileProperty,
    override val certFile: RegularFileProperty,
    override val keyFile: RegularFileProperty
) : ConfigurableHelmInstallFromRepositoryOptions,
    ConfigurableHelmInstallationOptions by installationOptions {

    constructor(objects: ObjectFactory)
            : this(
        installationOptions = HelmInstallationOptionsHolder(objects),
        repository = objects.property(),
        username = objects.property(),
        password = objects.property(),
        caFile = objects.fileProperty(),
        certFile = objects.fileProperty(),
        keyFile = objects.fileProperty()
    )
}


internal object HelmInstallFromRepositoryOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmInstallFromRepositoryOptions) {
            with(spec) {

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
