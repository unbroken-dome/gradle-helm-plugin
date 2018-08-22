package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


/**
 * Creates a [Property] to hold values of the given type.
 *
 * @param T the type of the property
 * @return the property
 */
inline fun <reified T : Any> ObjectFactory.property(): Property<T> =
        property(T::class.java)


/**
 * Creates a [Property] to hold values of the given type, initializing with a value.
 *
 * @param T the type of the property
 * @param initialValue the initial value. If this is `null` then the property will not have a value intiially.
 * @return the property
 */
inline fun <reified T : Any> ObjectFactory.property(initialValue: T?): Property<T> =
        property<T>().apply { set(initialValue) }


/**
 * Creates a [Property] to hold values of the given type, initializing it with a provider.
 *
 * Equivalent to calling `set(initialProvider)` on the new property.
 *
 * @param T the type of the property
 * @param initialProvider the initial provider.
 * @return the property
 */
inline fun <reified T : Any> ObjectFactory.property(initialProvider: Provider<out T>): Property<T> =
        property<T>().apply { set(initialProvider) }


/**
 * Creates a new [ListProperty] to hold a list of values of the given type.
 *
 * @param T the type of element
 */
inline fun <reified T : Any> ObjectFactory.listProperty(): ListProperty<T> =
        listProperty(T::class.java)
