package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.HELM_CHARTS_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_REPOSITORIES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension


/**
 * Gets the [HelmExtension] that is installed on the project.
 */
internal val Project.helm: HelmExtension
    get() = extensions.getByType(HelmExtension::class.java)


/**
 * Gets the [Linting] sub-extension.
 */
internal val HelmExtension.lint: Linting
    get() = requiredExtension(HELM_LINT_EXTENSION_NAME)


/**
 * Gets the `repositories` sub-extension.
 */
internal val HelmExtension.repositories: NamedDomainObjectContainer<HelmRepository>
    get() = requiredExtension(HELM_REPOSITORIES_EXTENSION_NAME)


/**
 * Gets the `charts` sub-extension.
 */
internal val HelmExtension.charts: NamedDomainObjectContainer<HelmChart>
    get() = requiredExtension(HELM_CHARTS_EXTENSION_NAME)


/**
 * Gets the [Filtering] sub-extension.
 */
internal val HelmExtension.filtering: Filtering
    get() = requiredExtension(HELM_FILTERING_EXTENSION_NAME)
