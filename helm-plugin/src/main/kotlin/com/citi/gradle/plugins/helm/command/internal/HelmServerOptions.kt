package com.citi.gradle.plugins.helm.command.internal

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableHelmServerOptions
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmOptions
import com.citi.gradle.plugins.helm.command.HelmServerOptions
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.withDefault


fun HelmServerOptions.withDefaults(
    defaults: HelmServerOptions, providers: ProviderFactory
): HelmServerOptions =
    object : HelmServerOptions {
        override val kubeConfig: Provider<RegularFile>
            get() = this@withDefaults.kubeConfig.withDefault(defaults.kubeConfig, providers)

        override val kubeContext: Provider<String>
            get() = this@withDefaults.kubeContext.withDefault(defaults.kubeContext, providers)

        override val namespace: Provider<String>
            get() = this@withDefaults.namespace.withDefault(defaults.namespace, providers)
    }


fun ConfigurableHelmServerOptions.conventionsFrom(source: HelmServerOptions) = apply {
    kubeConfig.convention(source.kubeConfig)
    kubeContext.convention(source.kubeContext)
    namespace.convention(source.namespace)
}


fun ConfigurableHelmServerOptions.setFrom(source: HelmServerOptions) = apply {
    kubeConfig.set(source.kubeConfig)
    kubeContext.set(source.kubeContext)
    namespace.set(source.namespace)
}


data class HelmServerOptionsHolder(
    override val kubeContext: Property<String>,
    override val kubeConfig: RegularFileProperty,
    override val namespace: Property<String>
) : ConfigurableHelmServerOptions {

    constructor(objects: ObjectFactory)
    : this(
        kubeContext = objects.property(),
        kubeConfig = objects.fileProperty(),
        namespace = objects.property()
    )
}


object HelmServerOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmServerOptions) {

            logger.debug("Applying options: {}", options)

            with(spec) {
                option("--kube-context", options.kubeContext)
                option("--namespace", options.namespace)
                environment("KUBECONFIG", options.kubeConfig)
            }
        }
    }
}
