package org.unbrokendome.gradle.plugins.helm.util

import groovy.lang.Closure
import groovy.util.Eval
import org.gradle.api.provider.Provider
import java.net.URI


/**
 * Executes the given action if the provider has a value; otherwise does nothing.
 *
 * Equivalent to `orNull?.let(action)`.
 *
 * @receiver the provider
 * @param action the action to execute on the provider's value if it is present
 */
internal inline fun <T : Any> Provider<T>.ifPresent(action: (T) -> Unit) {
    this.orNull?.let(action)
}


/**
 * Returns a new [Provider] that evaluates the value of this provider as a Groovy GString.
 *
 * @receiver the provider of a string value
 * @param evalRoot the context from which to evaluate any property references in the GString
 * @return the new [Provider], returning the evaluated GString
 */
internal fun Provider<String>.asGString(evalRoot: Any): Provider<String> =
    map { value ->
        @Suppress("UNCHECKED_CAST")
        val closure = Eval.me("{ -> \"${value.replace("\"", "\\\"")}\" }") as Closure<CharSequence>
        closure.delegate = evalRoot
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call().toString()
    }


/**
 * Returns a new [Provider] that converts the value of this provider to an URI.
 *
 * @receiver the provider of a string value
 * @return the new [Provider], returning the URI
 */
internal fun Provider<String>.toUri(): Provider<URI> =
    map(URI::create)
