package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.ExtensionAware
import java.security.cert.Extension


/**
 * Gets the extension of the given name if it exists.
 *
 * @param name the extension name
 * @return the extension, or `null` if it does not exist
 * @throws ClassCastException if the receiver object is not [ExtensionAware]
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Any.extension(name: String): T? =
        (this as ExtensionAware).extensions.findByName(name) as T?


/**
 * Gets the extension of the given name, throwing an exception if it does not exist.
 *
 * @param name the extension name
 * @throws ClassCastException if the receiver object is not [ExtensionAware]
 * @throws org.gradle.api.UnknownDomainObjectException if the extension does not exist
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Any.requiredExtension(name: String): T =
        (this as ExtensionAware).extensions.getByName(name) as T


/**
 * Gets the extension of the given type, throwing an exception if it does not exist.
 *
 * @param <T> the extension type
 * @throws ClassCastException if the receiver object is not [ExtensionAware]
 * @throws org.gradle.api.UnknownDomainObjectException if the extension does not exist
 */
internal inline fun <reified T : Any> Any.requiredExtension(): T =
        (this as ExtensionAware).extensions.getByType(T::class.java)


/**
 * Gets the convention of the given type, throwing an exception if it does not exist.
 *
 * @param <T> the convention type
 * @throws ClassCastException if the receiver object does not support conventions
 * @throws org.gradle.api.UnknownDomainObjectException if the convention does not exist
 */
internal inline fun <reified T : Any> Any.requiredConvention(): T =
        (this as HasConvention).convention.getByType(T::class.java)
