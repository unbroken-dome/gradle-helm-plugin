package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.internal.provider.AbstractProvider


/**
 * A simple [org.gradle.api.provider.Provider] implementation that always returns a fixed value.
 */
internal class FixedValueProvider<T : Any>(
        private val value: T)
    : AbstractProvider<T>() {


    override fun getType(): Class<T> =
            value.javaClass


    override fun isPresent(): Boolean =
            true


    override fun getOrNull(): T =
            value
}
