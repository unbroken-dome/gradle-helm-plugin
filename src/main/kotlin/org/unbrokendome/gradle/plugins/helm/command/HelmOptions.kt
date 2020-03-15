package org.unbrokendome.gradle.plugins.helm.command


interface HelmOptions


/**
 * Strategy interface to apply [HelmOptions] to a [HelmExecSpec].
 */
internal interface HelmOptionsApplier {

    /**
     * Applies a set of options to a [HelmExecSpec].
     *
     * @param spec the [HelmExecSpec]
     * @param options the options to apply
     */
    fun apply(spec: HelmExecSpec, options: HelmOptions)


    /**
     * A list of other [HelmOptionsApplier]s that must also be applied when this [HelmOptionsApplier] is used.
     */
    val implies: List<HelmOptionsApplier>
        get() = emptyList()
}
