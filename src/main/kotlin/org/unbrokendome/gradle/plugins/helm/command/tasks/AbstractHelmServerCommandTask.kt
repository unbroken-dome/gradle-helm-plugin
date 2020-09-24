package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmServerOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOptionsApplier


/**
 * Base class for tasks representing Helm CLI commands that communicate with the remote Kubernetes cluster.
 */
abstract class AbstractHelmServerCommandTask : AbstractHelmCommandTask(), ConfigurableHelmServerOptions {

    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsApplier(HelmServerOptionsApplier)
}
