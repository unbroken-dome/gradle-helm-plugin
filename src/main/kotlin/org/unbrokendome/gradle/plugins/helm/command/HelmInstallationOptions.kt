package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.withDefault


interface HelmInstallationOptions : HelmServerOperationOptions {

    val atomic: Provider<Boolean>

    val devel: Provider<Boolean>

    val verify: Provider<Boolean>

    val wait: Provider<Boolean>

    val version: Provider<String>

    val createNamespace: Provider<Boolean>
}


fun HelmInstallationOptions.withDefaults(
    defaults: HelmInstallationOptions, providers: ProviderFactory
): HelmInstallationOptions =
    object : HelmInstallationOptions,
        HelmServerOperationOptions by withDefaults(defaults as HelmServerOperationOptions, providers) {

        override val atomic: Provider<Boolean>
            get() = this@withDefaults.atomic.withDefault(defaults.atomic, providers)

        override val devel: Provider<Boolean>
            get() = this@withDefaults.devel.withDefault(defaults.devel, providers)

        override val verify: Provider<Boolean>
            get() = this@withDefaults.verify.withDefault(defaults.verify, providers)

        override val wait: Provider<Boolean>
            get() = this@withDefaults.wait.withDefault(defaults.wait, providers)

        override val version: Provider<String>
            get() = this@withDefaults.version.withDefault(defaults.version, providers)

        override val createNamespace: Provider<Boolean>
            get() = this@withDefaults.createNamespace.withDefault(defaults.createNamespace, providers)
    }


interface ConfigurableHelmInstallationOptions : ConfigurableHelmServerOperationOptions, HelmInstallationOptions {

    /**
     * If `true`, roll back changes on failure.
     *
     * Corresponds to the `--atomic` Helm CLI parameter.
     */
    @get:Internal
    override val atomic: Property<Boolean>


    /**
     * If `true`, use development versions, too. Equivalent to version `>0.0.0-0`.
     *
     * Corresponds to the `--devel` CLI parameter.
     */
    @get:[Input Optional]
    override val devel: Property<Boolean>


    /**
     * If `true`, verify the package before installing it.
     *
     * Corresponds to the `--verify` CLI parameter.
     */
    @get:Internal
    override val verify: Property<Boolean>


    /**
     * If `true`, will wait until all Pods, PVCs, Services, and minimum number of Pods of a Deployment are in a ready
     * state before marking the release as successful. It will wait for as long as [remoteTimeout].
     */
    @get:Internal
    override val wait: Property<Boolean>


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
     *
     * Corresponds to the `--version` CLI parameter.
     */
    @get:[Input Optional]
    override val version: Property<String>


    /**
     * If `true`, create the release namespace if not present.
     *
     * Corresponds to the `--create-namespace` CLI parameter.
     */
    @get:Internal
    override val createNamespace: Property<Boolean>
}


internal fun ConfigurableHelmInstallationOptions.conventionsFrom(source: HelmInstallationOptions) {
    conventionsFrom(source as HelmServerOperationOptions)
    atomic.convention(source.atomic)
    devel.convention(source.devel)
    verify.convention(source.verify)
    wait.convention(source.wait)
    version.convention(source.version)
    createNamespace.convention(source.createNamespace)
}


internal fun ConfigurableHelmInstallationOptions.setFrom(source: HelmInstallationOptions) {
    setFrom(source as HelmServerOperationOptions)
    atomic.set(source.atomic)
    devel.set(source.devel)
    verify.set(source.verify)
    wait.set(source.wait)
    version.set(source.version)
    createNamespace.set(source.createNamespace)
}


internal object HelmInstallationOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmInstallationOptions) {

            logger.debug("Applying options: {}", options)

            with(spec) {
                flag("--atomic", options.atomic)
                flag("--devel", options.devel)
                flag("--verify", options.verify)
                flag("--wait", options.wait)
                option("--version", options.version)
                flag("--create-namespace", options.createNamespace)
            }
        }
    }
}
