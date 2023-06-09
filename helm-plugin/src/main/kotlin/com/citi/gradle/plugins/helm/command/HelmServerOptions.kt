package com.citi.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


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
