package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.toHelmString
import java.time.Duration


/**
 * Base class for tasks representing Helm CLI commands that communicate with the remote Kubernetes cluster.
 */
abstract class AbstractHelmServerCommandTask : AbstractHelmCommandTask() {

    /**
     * Path to the Kubernetes configuration file.
     *
     * If this property is set, its value will be used to set the `KUBECONFIG` environment variable for each
     * Helm invocation.
     */
    @get:[Input Optional]
    val kubeConfig: RegularFileProperty =
        project.objects.fileProperty()
            .convention(project.helm.kubeConfig)


    /**
     * Name of the kubeconfig context to use.
     *
     * Corresponds to the `--kube-context` command line option in the Helm CLI.
     */
    @get:[Input Optional]
    val kubeContext: Property<String> =
        project.objects.property<String>()
            .convention(project.helm.kubeContext)


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    val remoteTimeout: Property<Duration> =
        project.objects.property<Duration>()
            .convention(project.helm.remoteTimeout)


    /**
     * Namespace scope for this request.
     *
     * Corresponds to the `--namespace` CLI parameter.
     */
    @get:Internal
    val namespace: Property<String> =
        project.objects.property()


    override fun modifyHelmExecSpec(exec: HelmExecSpec) = exec.run {
        option("--kube-context", kubeContext)
        option("--namespace", namespace)
        option("--timeout", remoteTimeout.map { it.toHelmString() })
        environment("KUBECONFIG", kubeConfig)
    }
}
