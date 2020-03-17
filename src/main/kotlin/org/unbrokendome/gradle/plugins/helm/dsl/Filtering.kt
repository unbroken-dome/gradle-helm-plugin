package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import javax.inject.Inject


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
     * Values to be inserted for placeholders.
     */
    @get:Input
    val values: MapProperty<String, Any>

    /**
     * Values to be inserted for placeholders, read from the contents of files.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Values of type
     * [org.gradle.api.file.FileCollection] are also allowed, provided that they contain only a single file.
     *
     * Additionally, when adding a [Provider] that represents an output file of another task, the corresponding
     * filtering task will automatically have a task dependency on the producing task.
     *
     * If the same key is present both in [values] and `fileValues`, then the entry from [values] has precedence.
     */
    @get:Internal
    val fileValues: MapProperty<String, Any>
}


/**
 * Extension of [Filtering] that supports setting values from a parent `Filtering` instance.
 */
internal interface FilteringInternal : Filtering, Hierarchical<Filtering>


/**
 * Default implementation of [Filtering].
 */
private open class DefaultFiltering
@Inject constructor(
    objects: ObjectFactory
) : FilteringInternal {

    final override val enabled: Property<Boolean> =
        objects.property()


    final override val values: MapProperty<String, Any> =
        objects.mapProperty<String, Any>().empty()


    final override val fileValues: MapProperty<String, Any> =
        objects.mapProperty<String, Any>().empty()


    final override fun setParent(parent: Filtering) {
        enabled.convention(parent.enabled)
        values.putAll(parent.values)
        fileValues.putAll(parent.fileValues)
    }
}


/**
 * Creates a new [Filtering] object using the given [ObjectFactory].
 *
 * @receiver the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Filtering] object
 * @return the created [Filtering] object
 */
internal fun ObjectFactory.createFiltering(parent: Filtering? = null): Filtering =
    newInstance(DefaultFiltering::class.java)
        .apply {
            parent?.let(this::setParent)
        }
