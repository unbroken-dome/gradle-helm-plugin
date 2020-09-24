package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmServerOperationOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmServerOperationOptionsApplier


/**
 * Base class for tasks that call a Helm CLI command representing an operation on the server
 * (`install`, `uninstall`, `upgrade`).
 */
abstract class AbstractHelmServerOperationCommandTask :
    AbstractHelmServerCommandTask(), ConfigurableHelmServerOperationOptions {

    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsApplier(HelmServerOperationOptionsApplier)
}
