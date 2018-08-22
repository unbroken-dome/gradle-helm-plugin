package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension


/**
 * Gets the [HelmExtension] that is installed on the project.
 */
val Project.helm: HelmExtension
    get() = extensions.getByType(HelmExtension::class.java)


/**
 * Gets the [Linting] sub-extension.
 */
internal val HelmExtension.lint: Linting
    get() = requiredExtension("lint")


/**
 * Gets the [Filtering] sub-extension.
 */
internal val HelmExtension.filtering: Filtering
    get() = requiredExtension("filtering")
