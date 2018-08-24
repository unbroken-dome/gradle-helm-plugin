package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.unbrokendome.gradle.plugins.helm.util.MapProperty
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import javax.inject.Inject


interface Filtering {

    @get:Input
    val enabled: Property<Boolean>

    @get:Input
    val placeholderPrefix: Property<String>

    @get:Input
    val placeholderSuffix: Property<String>

    @get:Input
    val values: MapProperty<String, Any?>
}


internal interface FilteringInternal : Filtering, Hierarchical<Filtering>


private open class DefaultFiltering
@Inject constructor(objectFactory: ObjectFactory) : FilteringInternal {

    override val enabled = objectFactory.property(true)

    override val placeholderPrefix = objectFactory.property("\${")

    override val placeholderSuffix = objectFactory.property("}")

    override val values = mapProperty<String, Any?>()

    override fun setParent(parent: Filtering) {
        enabled.set(parent.enabled)
        placeholderPrefix.set(parent.placeholderPrefix)
        placeholderSuffix.set(parent.placeholderSuffix)
        values.putAll(parent.values)
    }
}


internal fun createFiltering(objectFactory: ObjectFactory, parent: Filtering? = null): Filtering =
        objectFactory.newInstance(DefaultFiltering::class.java)
                .apply {
                    parent?.let(this::setParent)
                }
