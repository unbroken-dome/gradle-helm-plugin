package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal


/**
 * Configures filtering of chart sources.
 */
interface Filtering {

    companion object {
        @JvmStatic
        val DEFAULT_FILE_PATTERNS = listOf("Chart.yaml", "values.yaml", "requirements.yaml")
    }


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

    /**
     * Patterns of file names to be filtered.
     *
     * If this is an empty list, all files in the chart will be subject to filtering.
     *
     * By default, this includes `Chart.yaml`, `values.yaml` and `requirements.yaml` but not any
     * of the chart's template files. For the latter, it is recommended to put the filtered values
     * into values.yaml and apply Helm's templating mechanisms in the template files.
     */
    @get:Input
    val filePatterns: ListProperty<String>
}


internal fun Filtering.setParent(parent: Filtering) {
    enabled.convention(parent.enabled)
    values.putAll(parent.values)
    fileValues.putAll(parent.fileValues)

    // Inherit by convention here instead of addAll, so we can selectively override
    // the entire list in a child filtering block
    filePatterns.convention(parent.filePatterns)
}


/**
 * Creates a new [Filtering] object using the given [ObjectFactory].
 *
 * @receiver the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Filtering] object
 * @return the created [Filtering] object
 */
internal fun ObjectFactory.createFiltering(parent: Filtering? = null): Filtering =
    newInstance(Filtering::class.java)
        .apply {
            values.empty()
            fileValues.empty()
            filePatterns.convention(Filtering.DEFAULT_FILE_PATTERNS)
            parent?.let { setParent(it) }
        }
