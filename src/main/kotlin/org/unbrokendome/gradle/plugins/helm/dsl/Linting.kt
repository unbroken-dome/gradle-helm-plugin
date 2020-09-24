package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


/**
 * Defines options for linting Helm charts using the `helm lint` command.
 */
interface Linting {

    /**
     * If `true` (the default), run the linter.
     */
    val enabled: Property<Boolean>

    /**
     * If `true`, treat warnings from the linter as errors.
     *
     * Corresponds to the `--strict` CLI option.
     */
    val strict: Property<Boolean>

    /**
     * Values to be used by the linter.
     *
     * Entries in the map will be sent to the CLI using either the `--set-string` option (for strings) or the
     * `--set` option (for all other types).
     */
    val values: MapProperty<String, Any>

    /**
     * Values read from the contents of files, to be used by the linter.
     *
     * Corresponds to the `--set-file` CLI option.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Additionally, when adding a
     * [Provider] that represents an output file of another task, the corresponding lint task will
     * automatically have a task dependency on the producing task.
     *
     * Not to be confused with [valueFiles], which contains a collection of YAML files that supply multiple values.
     */
    val fileValues: MapProperty<String, Any>

    /**
     * A collection of YAML files containing values to be used by the linter.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    val valueFiles: ConfigurableFileCollection
}


internal fun Linting.setParent(parent: Linting) {
    enabled.set(parent.enabled)
    strict.set(parent.strict)
    values.putAll(parent.values)
    fileValues.putAll(parent.fileValues)
    valueFiles.from(parent.valueFiles)
}


/**
 * Creates a new [Linting] object using the given [ObjectFactory].
 *
 * @receiver the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Linting] object
 * @return the created [Linting] object
 */
internal fun ObjectFactory.createLinting(parent: Linting? = null): Linting =
    newInstance(Linting::class.java)
        .apply {
            enabled.convention(true)
            parent?.let(this::setParent)
        }
