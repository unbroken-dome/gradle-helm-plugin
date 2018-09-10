package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.dsl.tiller
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Base class for tasks representing Helm CLI commands that communicate with Tiller.
 */
abstract class AbstractHelmServerCommandTask : AbstractHelmCommandTask() {

    /**
     * Address of Tiller, in the format `host:port`.
     *
     * If this property is set, its value will be used to set the `HELM_HOST` environment variable for each
     * Helm invocation.
     */
    @get:[Input Optional]
    val host: Property<String> =
            project.objects.property(project.helm.host)


    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    @get:[Input Optional]
    val kubeConfig: RegularFileProperty =
            project.layout.fileProperty(project.helm.kubeConfig)


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    @get:[Input Optional]
    val kubeContext: Property<String> =
            project.objects.property(project.helm.kubeContext)


    /**
     * Namespace of Tiller.
     *
     * If this property is set, its value will be used to set the `TILLER_NAMESPACE` environment variable for
     * each Helm invocation.
     */
    @get:[Input Optional]
    val tillerNamespace: Property<String> =
            project.objects.property(project.helm.tiller.namespace)


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    val timeoutSeconds: Property<Int> =
            project.objects.property(project.helm.timeoutSeconds)


    override fun HelmExecSpec.modifyHelmExecSpec() {
        option("--kube-context", kubeContext)
        option("--timeout", timeoutSeconds)
        environment("HELM_HOST", host)
        environment("TILLER_NAMESPACE", tillerNamespace)
        environment("KUBECONFIG", kubeConfig)
    }
}
