package com.citi.gradle.plugins.helm.command.internal

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableHelmServerOperationOptions
import com.citi.gradle.plugins.helm.command.ConfigurableHelmServerOptions
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmOptions
import com.citi.gradle.plugins.helm.command.HelmServerOperationOptions
import com.citi.gradle.plugins.helm.command.HelmServerOptions
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.toSimpleString
import org.unbrokendome.gradle.pluginutils.withDefault
import java.time.Duration


fun HelmServerOperationOptions.withDefaults(
    defaults: HelmServerOperationOptions, providers: ProviderFactory
): HelmServerOperationOptions =
    object : HelmServerOperationOptions,
        HelmServerOptions by withDefaults(defaults as HelmServerOptions, providers) {

        override val dryRun: Provider<Boolean>
            get() = this@withDefaults.dryRun.withDefault(defaults.dryRun, providers)

        override val noHooks: Provider<Boolean>
            get() = this@withDefaults.noHooks.withDefault(defaults.noHooks, providers)

        override val remoteTimeout: Provider<Duration>
            get() = this@withDefaults.remoteTimeout.withDefault(defaults.remoteTimeout, providers)
    }


fun ConfigurableHelmServerOperationOptions.conventionsFrom(source: HelmServerOperationOptions) = apply {
    conventionsFrom(source as HelmServerOptions)
    dryRun.convention(source.dryRun)
    noHooks.convention(source.noHooks)
    remoteTimeout.convention(source.remoteTimeout)
}


fun ConfigurableHelmServerOperationOptions.setFrom(source: HelmServerOperationOptions) = apply {
    setFrom(source as HelmServerOptions)
    dryRun.set(source.dryRun)
    noHooks.set(source.noHooks)
    remoteTimeout.set(source.remoteTimeout)
}


data class HelmServerOperationOptionsHolder(
    private val serverOptions: ConfigurableHelmServerOptions,
    override val dryRun: Property<Boolean>,
    override val noHooks: Property<Boolean>,
    override val remoteTimeout: Property<Duration>
) : ConfigurableHelmServerOperationOptions,
    ConfigurableHelmServerOptions by serverOptions {

    constructor(objects: ObjectFactory)
    : this(
        serverOptions = HelmServerOptionsHolder(objects),
        dryRun = objects.property(),
        noHooks = objects.property(),
        remoteTimeout = objects.property()
    )
}


object HelmServerOperationOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmServerOperationOptions) {
            with(spec) {

                logger.debug("Applying options: {}", options)

                flag("--dry-run", options.dryRun)
                flag("--no-hooks", options.noHooks)
                option("--timeout", options.remoteTimeout.map { it.toSimpleString() })
            }
        }
    }
}
