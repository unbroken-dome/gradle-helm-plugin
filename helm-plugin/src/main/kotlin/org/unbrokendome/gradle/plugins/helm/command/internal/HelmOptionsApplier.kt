package com.citi.gradle.plugins.helm.command.internal

import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmOptions


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
