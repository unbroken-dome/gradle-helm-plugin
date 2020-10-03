package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.toHelmString
import org.unbrokendome.gradle.plugins.helm.util.withDefault
import java.time.Duration


interface HelmServerOperationOptions : HelmServerOptions {

    val dryRun: Provider<Boolean>

    val noHooks: Provider<Boolean>

    val remoteTimeout: Provider<Duration>
}


internal fun HelmServerOperationOptions.withDefaults(
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


interface ConfigurableHelmServerOperationOptions : ConfigurableHelmServerOptions, HelmServerOperationOptions {

    /**
     * If `true`, only simulate the operation.
     *
     * Corresponds to the `--dry-run` CLI parameter.
     */
    @get:Internal
    override val dryRun: Property<Boolean>


    /**
     * If `true`, prevent hooks from running during the operation.
     *
     * Corresponds to the `--no-hooks` CLI parameter.
     */
    @get:Internal
    override val noHooks: Property<Boolean>


    /**
     * Time to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
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
