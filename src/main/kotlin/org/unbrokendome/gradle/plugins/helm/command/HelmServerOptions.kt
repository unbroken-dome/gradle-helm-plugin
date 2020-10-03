package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.withDefault


interface HelmServerOptions : HelmOptions {

    val kubeConfig: Provider<RegularFile>

    val kubeContext: Provider<String>

    val namespace: Provider<String>
}


internal fun HelmServerOptions.withDefaults(
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


interface ConfigurableHelmServerOptions : HelmServerOptions, ConfigurableHelmOptions {

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    @get:[InputFile Optional]
    override val kubeConfig: RegularFileProperty


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    @get:[Input Optional]
    override val kubeContext: Property<String>


    /**
     * Namespace scope for this request.
     *
     * Corresponds to the `--namespace` CLI parameter.
     */
    @get:Internal
    override val namespace: Property<String>
}


internal fun ConfigurableHelmServerOptions.conventionsFrom(source: HelmServerOptions) = apply {
    kubeConfig.convention(source.kubeConfig)
    kubeContext.convention(source.kubeContext)
    namespace.convention(source.namespace)
}


internal fun ConfigurableHelmServerOptions.setFrom(source: HelmServerOptions) = apply {
    kubeConfig.set(source.kubeConfig)
    kubeContext.set(source.kubeContext)
    namespace.set(source.namespace)
}


internal object HelmServerOptionsApplier : HelmOptionsApplier {

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
