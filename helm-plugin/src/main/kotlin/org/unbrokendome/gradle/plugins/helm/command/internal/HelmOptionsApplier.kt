package org.unbrokendome.gradle.plugins.helm.command.internal

import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.command.HelmOptions


/**
 * Strategy interface to apply [HelmOptions] to a [HelmExecSpec].
 */
interface HelmOptionsApplier {

    /**
     * Applies a set of options to a [HelmExecSpec].
     *
     * @param spec the [HelmExecSpec]
     * @param options the options to apply
     */
    fun apply(spec: HelmExecSpec, options: HelmOptions)
}
