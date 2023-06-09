package com.citi.gradle.plugins.helm.command.internal

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableHelmInstallFromRepositoryOptions
import com.citi.gradle.plugins.helm.command.ConfigurableHelmInstallationOptions
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmInstallFromRepositoryOptions
import com.citi.gradle.plugins.helm.command.HelmInstallationOptions
import com.citi.gradle.plugins.helm.command.HelmOptions
import org.unbrokendome.gradle.pluginutils.property
import java.net.URI


fun ConfigurableHelmInstallFromRepositoryOptions.setFrom(source: HelmInstallFromRepositoryOptions) {
    setFrom(source as HelmInstallationOptions)
    repository.set(source.repository)
    username.set(source.username)
    password.set(source.password)
    caFile.set(source.caFile)
    certFile.set(source.certFile)
    keyFile.set(source.keyFile)
}


data class HelmInstallFromRepositoryOptionsHolder(
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


object HelmInstallFromRepositoryOptionsApplier : HelmOptionsApplier {

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
