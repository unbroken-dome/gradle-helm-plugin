package org.unbrokendome.gradle.plugins.helm.testutil

import assertk.Assert
import assertk.all
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import java.io.File


inline fun <reified E : Any> Assert<*>.hasExtension(name: String? = null, noinline block: (Assert<E>) -> Unit = {}) {
    if (actual !is ExtensionAware) {
        expected("to be ExtensionAware")
    }
    val extensions = (actual as ExtensionAware).extensions

    val extension: E? = if (name != null) {
        extensions.findByName(name)
                .let {
                    if (it == null) {
                        expected("to have an extension named \"$name\" of type ${show(E::class)}")
                    }
                    if (it !is E) {
                        expected("to have an extension named \"$name\" of type ${show(E::class)}, but actual type was: ${show(it?.javaClass)}")
                    }
                    it as E
                }
    } else {
        extensions.findByType(E::class.java)
                .also {
                    if (it == null) {
                        expected("to have an extension of type ${show(E::class)}")
                    }
                }
    }

    assert(extension!!, name = "extension").all(block)
}



fun <T : Any> Assert<NamedDomainObjectCollection<T>>.containsItem(name: String, block: (Assert<T>) -> Unit = {}) {
    val item = actual.findByName(name)
    if (item == null) {
        expected("to contain an item named \"$name\"")
    }
    assert(item!!, name = name).all(block)
}



fun <T : Any> Assert<Provider<T>>.isPresent(block: (Assert<T>) -> Unit = {}) {
    if (!actual.isPresent) {
        expected("${show(actual)} to have a value", actual = actual)
    }
    assert(actual.get()).all(block)
}
