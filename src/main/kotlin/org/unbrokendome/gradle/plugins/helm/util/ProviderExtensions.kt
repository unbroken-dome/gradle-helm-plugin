package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.provider.Provider


/**
 * Executes the given action if the provider has a value; otherwise does nothing.
 *
 * Equivalent to `orNull?.let(action)`.
 */
inline fun <T : Any> Provider<T>.ifPresent(action: (T) -> Unit) {
    this.orNull?.let(action)
}
