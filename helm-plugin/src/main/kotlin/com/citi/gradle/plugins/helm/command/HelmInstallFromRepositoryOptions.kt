package com.citi.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
