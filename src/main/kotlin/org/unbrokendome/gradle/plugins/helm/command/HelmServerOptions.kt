package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider


/**
 * Options that are relevant for Helm CLI commands communicating with Tiller (e.g. `helm install`).
 */
interface HelmServerOptions {

    /**
     * Address of Tiller, in the format `host:port`.
     *
     * If this property is set, its value will be used to set the `HELM_HOST` environment variable for each
     * Helm invocation.
     */
    val host: Provider<String>

    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    val kubeContext: Provider<String>

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    val kubeConfig: Provider<RegularFile>

    /**
     * Namespace of Tiller.
     *
     * If this property is set, its value will be used to set the `TILLER_NAMESPACE` environment variable for
     * each Helm invocation.
     */
    val tillerNamespace: Provider<String>
}
