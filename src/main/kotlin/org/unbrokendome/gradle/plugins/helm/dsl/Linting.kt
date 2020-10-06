package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.unbrokendome.gradle.plugins.helm.util.GRADLE_VERSION_5_3
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import javax.inject.Inject


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


/**
 * Extension of [Linting] that supports setting values from a parent `Linting` instance.
 */
private interface LintingInternal : Linting, Hierarchical<Linting>


/**
 * Default implementation of [Linting].
 */
private open class DefaultLinting
@Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : LintingInternal {

    final override val enabled: Property<Boolean> =
        objectFactory.property<Boolean>()
            .convention(true)

    final override val strict: Property<Boolean> =
        objectFactory.property()

    final override val values: MapProperty<String, Any> =
        objectFactory.mapProperty()

    override val fileValues: MapProperty<String, Any> =
        objectFactory.mapProperty()

    final override val valueFiles: ConfigurableFileCollection =
        if (GradleVersion.current() >= GRADLE_VERSION_5_3) {
            objectFactory.fileCollection()
        } else {
            @Suppress("DEPRECATION")
            projectLayout.configurableFiles()
        }


    final override fun setParent(parent: Linting) {
        enabled.set(parent.enabled)
        strict.set(parent.strict)
        values.putAll(parent.values)
        fileValues.putAll(parent.fileValues)
        valueFiles.from(parent.valueFiles)
    }
}


/**
 * Creates a new [Linting] object using the given [ObjectFactory].
 *
 * @receiver the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Linting] object
 * @return the created [Linting] object
 */
internal fun ObjectFactory.createLinting(parent: Linting? = null): Linting =
    newInstance(DefaultLinting::class.java)
        .apply {
            parent?.let(this::setParent)
        }
