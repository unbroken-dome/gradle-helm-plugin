package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.toHelmString
import java.time.Duration


interface HelmServerOperationOptions : HelmServerOptions {

    val dryRun: Provider<Boolean>

    val noHooks: Provider<Boolean>

    val remoteTimeout: Provider<Duration>
}


internal fun HelmServerOperationOptions.withDefaults(defaults: HelmServerOperationOptions): HelmServerOperationOptions =
    object : HelmServerOperationOptions,
        HelmServerOptions by withDefaults(defaults as HelmServerOptions) {

        override val dryRun: Provider<Boolean>
            get() = this@withDefaults.dryRun.orElse(defaults.dryRun)

        override val noHooks: Provider<Boolean>
            get() = this@withDefaults.noHooks.orElse(defaults.noHooks)

        override val remoteTimeout: Provider<Duration>
            get() = this@withDefaults.remoteTimeout.orElse(defaults.remoteTimeout)
    }


interface ConfigurableHelmServerOperationOptions : ConfigurableHelmServerOptions, HelmServerOperationOptions {

    /**
     * If `true`, only simulate the operation.
     *
     * Corresponds to the `--dry-run` CLI parameter.
     */
    override val dryRun: Property<Boolean>


    /**
     * If `true`, prevent hooks from running during the operation.
     *
     * Corresponds to the `--no-hooks` CLI parameter.
     */
    override val noHooks: Property<Boolean>


    /**
     * Time to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    override val remoteTimeout: Property<Duration>
}


internal fun ConfigurableHelmServerOperationOptions.conventionsFrom(source: HelmServerOperationOptions) = apply {
    conventionsFrom(source as HelmServerOptions)
    dryRun.convention(source.dryRun)
    noHooks.convention(source.noHooks)
    remoteTimeout.convention(source.remoteTimeout)
}


internal fun ConfigurableHelmServerOperationOptions.setFrom(source: HelmServerOperationOptions) = apply {
    setFrom(source as HelmServerOptions)
    dryRun.set(source.dryRun)
    noHooks.set(source.noHooks)
    remoteTimeout.set(source.remoteTimeout)
}


internal data class HelmServerOperationOptionsHolder(
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


internal object HelmServerOperationOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmServerOperationOptions) {
            with(spec) {

                logger.debug("Applying options: {}", options)

                flag("--dry-run", options.dryRun)
                flag("--no-hooks", options.noHooks)
                option("--timeout", options.remoteTimeout.map { it.toHelmString() })
            }
        }
    }
}
