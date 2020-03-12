package org.gradle.kotlin.dsl

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider


/**
 * Adds a map entry to the property value.
 *
 * Same as [MapProperty.put], but uses a different name to avoid an overload resolution ambiguity in case
 * the value type is `Any`.
 *
 * @param key the key
 * @param providerOfValue the provider of the value
 */
fun <K : Any, V : Any> MapProperty<K, V>.putFrom(key: K, providerOfValue: Provider<out V>) =
    put(key, providerOfValue)
