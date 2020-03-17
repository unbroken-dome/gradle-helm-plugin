package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOptionsApplier
import org.unbrokendome.gradle.plugins.helm.util.property
import java.time.Duration


/**
 * Base class for tasks representing Helm CLI commands that communicate with the remote Kubernetes cluster.
 */
abstract class AbstractHelmServerCommandTask : AbstractHelmCommandTask(), HelmServerOptions {

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    @get:[Input Optional]
    final override val kubeConfig: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    @get:[Input Optional]
    final override val kubeContext: Property<String> =
        project.objects.property()


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    final override val remoteTimeout: Property<Duration> =
        project.objects.property()


    /**
     * Namespace scope for this request.
     *
     * Corresponds to the `--namespace` CLI parameter.
     */
    @get:Internal
    final override val namespace: Property<String> =
        project.objects.property()


    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.withOptionsApplier(HelmServerOptionsApplier)
}
