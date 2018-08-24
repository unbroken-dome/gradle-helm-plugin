package org.unbrokendome.gradle.plugins.helm.testutil

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import kotlin.reflect.KClass
import assertk.assertions.isInstanceOf as defaultIsInstanceOf


inline fun <reified E : Any> Assert<*>.hasExtension(name: String? = null, noinline block: (Assert<E>) -> Unit = {}) {
    if (actual !is ExtensionAware) {
        return expected("to be ExtensionAware")
    }
    val extensions = (actual as ExtensionAware).extensions

    val extension: E = if (name != null) {
        extensions.findByName(name)
                .let {
                    if (it == null) {
                        return expected("to have an extension named \"$name\" of type ${show(E::class)}")
                    }
                    if (it !is E) {
                        return expected("to have an extension named \"$name\" of type ${show(E::class)}, but actual type was: ${show(it.javaClass)}")
                    }
                    it
                }
    } else {
        extensions.findByType(E::class.java)
                .let {
                    if (it == null) {
                        return expected("to have an extension of type ${show(E::class)}")
                    }
                    it
                }
    } as E

    assert(extension, name = "extension " + (name?.let { "\"$it\""} ?: show(E::class))).all(block)
}



fun <T : Any> Assert<NamedDomainObjectCollection<T>>.containsItem(name: String, block: (Assert<T>) -> Unit = {}) {
    val item = actual.findByName(name) ?: return expected("to contain an item named \"$name\"")
    assert(item, name = name).all(block)
}


fun <T : Any, S : T> Assert<T?>.isInstanceOf(kclass: KClass<S>, block: (Assert<S>) -> Unit) {
    isNotNull {
        it.defaultIsInstanceOf(kclass, block)
    }
}



fun <T : Any> Assert<Provider<T>>.isPresent(block: (Assert<T>) -> Unit = {}) {
    val value = actual.orNull
    if (value != null) {
        assert(value, name = name).all(block)
    } else {
        expected("${show(actual)} to have a value", actual = actual)
    }
}


fun <T : Any> Assert<Provider<T>>.hasValueEqualTo(value: T) {
    isPresent {
        it.isEqualTo(value)
    }
}
