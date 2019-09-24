package org.unbrokendome.gradle.plugins.helm.testutil

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import java.io.File
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
                    return expected(
                        "to have an extension named \"$name\" of type ${show(E::class)}, but actual type was: ${show(
                            it.javaClass
                        )}"
                    )
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

    assert(extension, name = "extension " + (name?.let { "\"$it\"" } ?: show(E::class))).all(block)
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


fun Assert<Provider<RegularFile>>.hasFileValueEqualTo(path: File) {
    isPresent {
        it.prop(RegularFile::getAsFile)
            .isEqualTo(path)
    }
}


fun Assert<Provider<RegularFile>>.hasFileValueEqualTo(path: String) {
    hasFileValueEqualTo(File(path))
}


fun Assert<Provider<Directory>>.hasDirValueEqualTo(path: File) {
    isPresent {
        it.prop(Directory::getAsFile)
            .isEqualTo(path)
    }
}


fun Assert<Provider<Directory>>.hasDirValueEqualTo(path: String) {
    hasDirValueEqualTo(File(path))
}


fun <T> Assert<List<T>>.at(index: Int, block: (Assert<T>) -> Unit = {}) {
    require(index >= 0)
    if (index >= actual.size) {
        return expected("to have an item at index $index but was:${show(actual)}")
    }
    val value = actual[index]
    assert(value, name = "[$index]").all(block)
}


fun <K, V> Assert<Map<K, V>>.hasEntry(key: K, block: (Assert<V>) -> Unit = {}) {
    val value = actual[key] ?: return expected("to contain an entry for key:${show(key)} but was:${show(actual)}")
    assert(value, name = "[${show(key)}]").all(block)
}


@Suppress("UNCHECKED_CAST")
fun <K, V> Assert<*>.isMapOf(block: (Assert<Map<K, V>>) -> Unit = {}) {
    isInstanceOf(Map::class) {
        assert(actual as Map<K, V>, name = name).run(block)
    }
}
