package org.unbrokendome.gradle.plugins.helm.util

import groovy.lang.Closure
import groovy.lang.GString
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
            val closure = Eval.me("{ -> \"${value.replace("\"", "\\\"")}\" }") as Closure<GString>
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


/**
 * Returns a new [Provider] that uses the given provider as a fallback if this provider does not have a value.
 *
 * The fallback provider may or may not have a value. If both this provider and the fallback provider have no value,
 * then the returned provider will not have a value either.
 *
 * @receiver the provider
 * @param fallbackProvider the fallback provider
 * @return the combined [Provider]
 */
internal inline fun <reified T : Any> Provider<T>.orElse(fallbackProvider: Provider<out T>): Provider<T> =
        FallbackProvider(T::class.java, this, fallbackProvider)


/**
 * Returns a new [Provider] that uses the given default value as a fallback if this provider does not have a value.
 *
 * @receiver the provider
 * @param defaultValue the default value
 * @return the combined [Provider]
 */
internal inline fun <reified T : Any> Provider<T>.orElse(defaultValue: T): Provider<T> =
        orElse(FixedValueProvider(defaultValue))
