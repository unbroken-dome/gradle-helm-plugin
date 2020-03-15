package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.util.toHelmString
import java.time.Duration


interface HelmServerOptions : GlobalHelmOptions {

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    val kubeConfig: RegularFileProperty


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    val kubeContext: Property<String>


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    val remoteTimeout: Property<Duration>


    /**
     * Namespace scope for this request.
     *
     * Corresponds to the `--namespace` CLI parameter.
     */
    val namespace: Property<String>
}


internal object HelmServerOptionsApplier : HelmOptionsApplier {

    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmServerOptions) {
            with(spec) {
                option("--kube-context", options.kubeContext)
                option("--namespace", options.namespace)
                option("--timeout", options.remoteTimeout.map { it.toHelmString() })
                environment("KUBECONFIG", options.kubeConfig)
            }
        }
    }

    override val implies: List<HelmOptionsApplier>
        get() = listOf(GlobalHelmOptionsApplier)
}
