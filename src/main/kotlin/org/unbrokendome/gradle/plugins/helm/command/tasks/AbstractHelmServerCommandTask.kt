package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOptions
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Base class for tasks representing Helm CLI commands that communicate with Tiller.
 */
abstract class AbstractHelmServerCommandTask : AbstractHelmCommandTask(), HelmServerOptions {

    @get:[Input Optional]
    override val host: Property<String> =
            project.objects.property(project.helm.host)


    @get:[Input Optional]
    override val kubeConfig: RegularFileProperty =
            project.layout.fileProperty(project.helm.kubeConfig)


    @get:[Input Optional]
    override val kubeContext: Property<String> =
            project.objects.property(project.helm.kubeContext)


    @get:[Input Optional]
    override val tillerNamespace: Property<String> =
            project.objects.property(project.helm.tillerNamespace)


    @get:Internal
    override val timeoutSeconds: Property<Int> =
            project.objects.property(project.helm.timeoutSeconds)


    override fun HelmExecSpec.modifyHelmExecSpec() {
        option("--kube-context", kubeContext)
        option("--timeout", timeoutSeconds)
        environment("HELM_HOST", host)
        environment("TILLER_NAMESPACE", tillerNamespace)
        environment("KUBECONFIG", kubeConfig)
    }
}
