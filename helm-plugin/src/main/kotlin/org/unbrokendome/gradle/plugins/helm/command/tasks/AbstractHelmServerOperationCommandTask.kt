package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmServerOperationOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOperationOptionsApplier
import org.unbrokendome.gradle.pluginutils.property
import java.time.Duration


/**
 * Base class for tasks that call a Helm CLI command representing an operation on the server
 * (`install`, `uninstall`, `upgrade`).
 */
abstract class AbstractHelmServerOperationCommandTask :
    AbstractHelmServerCommandTask(), ConfigurableHelmServerOperationOptions {

    /**
     * If `true`, only simulate the operation.
     *
     * Corresponds to the `--dry-run` CLI parameter.
     */
    @get:Internal
    final override val dryRun: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, prevent hooks from running during the operation.
     *
     * Corresponds to the `--no-hooks` CLI parameter.
     */
    @get:Internal
    final override val noHooks: Property<Boolean> =
        project.objects.property()


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    @get:Internal
    final override val remoteTimeout: Property<Duration> =
        project.objects.property()


    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsApplier(HelmServerOperationOptionsApplier)
}
