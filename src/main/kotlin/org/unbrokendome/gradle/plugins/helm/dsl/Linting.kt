package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import org.unbrokendome.gradle.plugins.helm.command.mergeValues


/**
 * Defines options for linting Helm charts using the `helm lint` command.
 */
interface Linting : ConfigurableHelmValueOptions {

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
}


internal fun Linting.setParent(parent: Linting) {
    mergeValues(parent)
    enabled.set(parent.enabled)
    strict.set(parent.strict)
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
