package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.property


interface HelmServerOptions : HelmOptions {

    val kubeConfig: Provider<RegularFile>

    val kubeContext: Provider<String>

    val namespace: Provider<String>
}


interface ConfigurableHelmServerOptions : HelmServerOptions, ConfigurableHelmOptions {

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    override val kubeConfig: RegularFileProperty


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    override val kubeContext: Property<String>


    /**
     * Namespace scope for this request.
     *
     * Corresponds to the `--namespace` CLI parameter.
     */
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


internal data class HelmServerOptionsHolder(
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
