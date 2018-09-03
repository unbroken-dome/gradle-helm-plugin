package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.internal.provider.AbstractProvider
import org.gradle.api.provider.Provider


/**
 * Implementation of [Provider] that delegates to another provider, or uses a second provider as fallback if the
 * first does not have a value.
 */
internal class FallbackProvider<T : Any>(
        private val type: Class<T>?,
        private val provider: Provider<T>,
        private val fallback: Provider<out T>) : AbstractProvider<T>() {

    override fun getType(): Class<T>? = type


    override fun isPresent(): Boolean =
            provider.isPresent || fallback.isPresent


    override fun getOrNull(): T? =
            provider.orNull ?: fallback.orNull
}
