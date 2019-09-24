package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import javax.inject.Inject


internal const val FILTERING_DEFAULT_PLACEHOLDER_PREFIX = "\${"
internal const val FILTERING_DEFAULT_PLACEHOLDER_SUFFIX = "}"


/**
 * Configures filtering of chart sources.
 */
interface Filtering {

    /**
     * Indicates if filtering is enabled. The default is `true`.
     */
    @get:Input
    val enabled: Property<Boolean>

    /**
     * The prefix for marking a placeholder in filtered files.
     *
     * Defaults to `${`.
     */
    @get:Input
    val placeholderPrefix: Property<String>

    /**
     * The suffix for marking a placeholder in filtered files.
     *
     * Defaults to `}`.
     */
    @get:Input
    val placeholderSuffix: Property<String>

    /**
     * Values to be resolved for placeholders.
     */
    @get:Input
    val values: MapProperty<String, Any>
}


/**
 * Extension of [Filtering] that supports setting values from a parent `Filtering` instance.
 */
internal interface FilteringInternal : Filtering, Hierarchical<Filtering>


/**
 * Default implementation of [Filtering].
 */
private open class DefaultFiltering
@Inject constructor(objectFactory: ObjectFactory) : FilteringInternal {

    override val enabled: Property<Boolean> =
        objectFactory.property<Boolean>()
            .convention(true)


    override val placeholderPrefix: Property<String> =
        objectFactory.property<String>()
            .convention(FILTERING_DEFAULT_PLACEHOLDER_PREFIX)


    override val placeholderSuffix: Property<String> =
        objectFactory.property<String>()
            .convention(FILTERING_DEFAULT_PLACEHOLDER_SUFFIX)


    override val values: MapProperty<String, Any> =
        objectFactory.mapProperty<String, Any>().empty()


    override fun setParent(parent: Filtering) {
        enabled.set(parent.enabled)
        placeholderPrefix.set(parent.placeholderPrefix)
        placeholderSuffix.set(parent.placeholderSuffix)
        values.putAll(parent.values)
    }
}


/**
 * Creates a new [Filtering] object using the given [ObjectFactory].
 *
 * @param objectFactory the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Filtering] object
 * @return the created [Filtering] object
 */
internal fun createFiltering(objectFactory: ObjectFactory, parent: Filtering? = null): Filtering =
    objectFactory.newInstance(DefaultFiltering::class.java)
        .apply {
            parent?.let(this::setParent)
        }
