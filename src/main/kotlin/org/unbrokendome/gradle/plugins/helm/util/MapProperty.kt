package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.internal.provider.AbstractProvider
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.util.LinkedList


/**
 * Represents a property whose type is a [Map] of key-value pairs.
 *
 * In addition to setting [Map] values directly, a map property can also be used to
 * combine the entries of multiple [Map]-valued providers.
 */
interface MapProperty<K : Any, V : Any?> : Property<Map<K, V>> {

    /**
     *
     */
    fun put(key: K, value: V)

    fun putFrom(key: K, providerOfValue: Provider<out V>)

    fun put(providerOfEntry: Provider<out Any>)

    fun putAll(providerOfEntries: Provider<out Map<K, V>>)
}


private class DefaultMapProperty<K : Any, V : Any?>(
        private val keyType: Class<out K>,
        private val valueType: Class<out V>)
    : AbstractProvider<Map<K, V>>(), MapProperty<K, V> {

    private companion object {

        @Suppress("UNCHECKED_CAST")
        fun <K : Any, V : Any?> emptyMapCollector(): Collector<K, V> = EmptyMap as Collector<K, V>

        @Suppress("UNCHECKED_CAST")
        fun <K : Any, V : Any?> noValueCollector(): Collector<K, V> = NoValue as Collector<K, V>
    }

    private var value: Collector<K, V> = emptyMapCollector()
    private val collectors: MutableList<Collector<K, V>> = LinkedList()


    override fun isPresent(): Boolean =
            value.isPresent && collectors.all { it.isPresent }


    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<Map<K, V>> =
            Map::class.java as Class<Map<K, V>>


    override fun get(): Map<K, V> =
        LinkedHashMap<K, V>(collectors.size + 1)
                .apply {
                    value.collectInto(this)
                    collectors.forEach { it.collectInto(this) }
                }


    override fun getOrNull(): Map<K, V>? =
        LinkedHashMap<K, V>(collectors.size + 1)
                .apply {
                    if (!value.maybeCollectInto(this)) {
                        return null
                    }
                    collectors.forEach { collector ->
                        if (!collector.maybeCollectInto(this)) {
                            return null
                        }
                    }
                }


    override fun set(value: Map<K, V>?) {
        collectors.clear()
        this.value = if (value != null) { EntriesFromMap(value) } else { noValueCollector() }
    }


    override fun set(provider: Provider<out Map<K, V>>?) {
        requireNotNull(provider) { "Cannot set the value of a property using a null provider." }
        collectors.clear()
        this.value = EntriesFromMapProvider(provider!!)
    }


    override fun put(key: K, value: V) {
        collectors.add(SingleEntry(key, value))
    }


    override fun putFrom(key: K, providerOfValue: Provider<out V>) {
        collectors.add(EntryWithValueFromProvider(key, providerOfValue))
    }


    override fun put(providerOfEntry: Provider<out Any>) {
        @Suppress("UNCHECKED_CAST")
        val providerOfPairEntry: Provider<Pair<K, V>> = providerOfEntry
                .map { value ->
                    when (value) {
                        is Pair<*, *> -> value as Pair<K, V>
                        is Map.Entry<*, *> -> (value.key as K to value.value as V)
                        is Map<*, *> -> {
                            if (value.size > 1) {
                                throw IllegalArgumentException("A Map returned by a Provider supplied to the put() " +
                                        "method must have one single element. Use putAll() instead.")
                            }
                            value.toList().first() as Pair<K, V>
                        }
                        else -> {
                            throw IllegalArgumentException("A Provider supplied to the put() method must return a" +
                                    "Pair, Map.Entry, or Map")
                        }
                    }
                }
        collectors.add(EntryFromProvider(providerOfPairEntry))
    }


    override fun putAll(providerOfEntries: Provider<out Map<K, V>>) {
        collectors.add(EntriesFromMapProvider(providerOfEntries))
    }


    override fun toString(): String {
        val valueState = when (value) {
            is EmptyMap -> "empty"
            is NoValue -> "undefined"
            else -> "defined"
        }
        return "Map($keyType -> $valueType, $valueState)"
    }


    private interface Collector<K : Any, V : Any?> {

        val isPresent: Boolean

        fun collectInto(map: MutableMap<K, V>)

        fun maybeCollectInto(map: MutableMap<K, V>): Boolean
    }


    private object EmptyMap : Collector<Any, Any?> {

        override val isPresent: Boolean
            get() = true

        override fun collectInto(map: MutableMap<Any, Any?>) {}

        override fun maybeCollectInto(map: MutableMap<Any, Any?>): Boolean = true
    }


    private object NoValue : Collector<Any, Any?> {

        override val isPresent: Boolean
            get() = false

        override fun collectInto(map: MutableMap<Any, Any?>) {
            throw IllegalStateException(Providers.NULL_VALUE)
        }

        override fun maybeCollectInto(map: MutableMap<Any, Any?>): Boolean = false
    }


    private class SingleEntry<K : Any, V : Any?>(
            private val key: K,
            private val value: V)
        : Collector<K, V> {

        override val isPresent: Boolean
            get() = true

        override fun collectInto(map: MutableMap<K, V>) {
            map[key] = value
        }

        override fun maybeCollectInto(map: MutableMap<K, V>): Boolean {
            map[key] = value
            return true
        }
    }


    private class EntryFromProvider<K : Any, V : Any?>(
            private val providerOfEntry: Provider<out Pair<K, V>>)
        : Collector<K, V> {

        override val isPresent: Boolean
            get() = providerOfEntry.isPresent

        override fun collectInto(map: MutableMap<K, V>) {
            providerOfEntry.get()
                    .let { map[it.first] = it.second }
        }

        override fun maybeCollectInto(map: MutableMap<K, V>): Boolean =
                providerOfEntry.orNull
                        ?.apply { map[first] = second } != null
    }


    private class EntryWithValueFromProvider<K : Any, V : Any?>(
            private val key: K,
            private val providerOfValue: Provider<out V>)
        : Collector<K, V> {

        override val isPresent: Boolean
            get() = providerOfValue.isPresent

        override fun collectInto(map: MutableMap<K, V>) {
            map[key] = providerOfValue.get()
        }

        override fun maybeCollectInto(map: MutableMap<K, V>): Boolean =
            providerOfValue.orNull
                    ?.apply { map[key] = this } != null
    }


    private class EntriesFromMapProvider<K : Any, V : Any?>(
            private val providerOfEntries: Provider<out Map<K, V>>)
        : Collector<K, V> {

        override val isPresent: Boolean
            get() = providerOfEntries.isPresent

        override fun collectInto(map: MutableMap<K, V>) {
            providerOfEntries.get()
                    .let { map.putAll(it) }
        }

        override fun maybeCollectInto(map: MutableMap<K, V>): Boolean =
                providerOfEntries.orNull
                        ?.apply { map.putAll(this) } != null
    }


    private class EntriesFromMap<K : Any, V : Any?>(
            private val entries: Map<K, V>)
        : Collector<K, V> {

        override val isPresent: Boolean
            get() = true

        override fun collectInto(map: MutableMap<K, V>) {
            map.putAll(entries)
        }

        override fun maybeCollectInto(map: MutableMap<K, V>): Boolean {
            map.putAll(entries)
            return true
        }
    }
}


internal fun <K : Any, V : Any?> mapProperty(keyType: Class<out K>, valueType: Class<out V>): MapProperty<K, V> =
        DefaultMapProperty(keyType, valueType)


internal inline fun <reified K : Any, reified V : Any?> mapProperty(): MapProperty<K, V> =
        mapProperty(K::class.java, V::class.java)
