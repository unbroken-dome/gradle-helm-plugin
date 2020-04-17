package org.unbrokendome.gradle.plugins.helm.util

import groovy.lang.Closure
import groovy.util.Eval
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.util.GradleVersion
import java.io.File
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


/**
 * Returns a new [Provider] that returns the path of the [FileSystemLocation] as a [File] object.
 *
 * @receiver the [Provider] of the [FileSystemLocation] (either a `Provider<RegularFile>` or `Provider<Directory`)
 */
internal fun <T : FileSystemLocation> Provider<T>.asFile(): Provider<File> =
    map { it.asFile }


internal fun <T : Any> Provider<T>.withDefault(value: T, providers: ProviderFactory): Provider<T> =
    if (GradleVersion.current() >= GRADLE_VERSION_5_6) {
        orElse(value)
    } else {
        providers.provider {
            this.orNull ?: value
        }
    }


internal fun <T : Any> Provider<T>.withDefault(provider: Provider<T>, providers: ProviderFactory): Provider<T> =
    if (GradleVersion.current() >= GRADLE_VERSION_5_6) {
        orElse(provider)
    } else {
        providers.provider {
            this.orNull ?: provider.orNull
        }
    }
