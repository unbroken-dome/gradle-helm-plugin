package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.*
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.ChartDependencyHandler
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension


/**
 * Gets the [HelmExtension] that is installed on the project.
 */
val Project.helm: HelmExtension
    get() = extensions.getByType(HelmExtension::class.java)


/**
 * Gets the [Linting] sub-extension.
 */
val HelmExtension.lint: Linting
    get() = requiredExtension(HELM_LINT_EXTENSION_NAME)


/**
 * Gets the `repositories` sub-extension.
 */
val HelmExtension.repositories: NamedDomainObjectContainer<HelmRepository>
    get() = requiredExtension(HELM_REPOSITORIES_EXTENSION_NAME)


/**
 * Gets the `charts` sub-extension.
 */
val HelmExtension.charts: NamedDomainObjectContainer<HelmChart>
    get() = requiredExtension(HELM_CHARTS_EXTENSION_NAME)


/**
 * Gets the [Filtering] sub-extension.
 */
val HelmExtension.filtering: Filtering
    get() = requiredExtension(HELM_FILTERING_EXTENSION_NAME)


/**
 * Gets the chart's [Linting] extension.
 */
val HelmChart.lint: Linting
    get() = requiredExtension(HELM_LINT_EXTENSION_NAME)


/**
 * Configures the chart linting.
 */
fun HelmChart.lint(configure: (Linting).() -> Unit) =
        (this as ExtensionAware).extensions.configure(HELM_LINT_EXTENSION_NAME, configure)


/**
 * Gets the chart's [Filtering] extension.
 */
val HelmChart.filtering: Filtering
    get() = requiredExtension(HELM_FILTERING_EXTENSION_NAME)


/**
 * Configures the chart filtering.
 */
fun HelmChart.filtering(configure: (Filtering).() -> Unit) =
        (this as ExtensionAware).extensions.configure(HELM_FILTERING_EXTENSION_NAME, configure)


/**
 * Gets the chart's `dependencies` extension.
 */
val HelmChart.dependencies: ChartDependencyHandler
    get() = requiredExtension(HELM_DEPENDENCIES_EXTENSION_NAME)


/**
 * Configures the chart dependencies.
 */
fun HelmChart.dependencies(configure: (ChartDependencyHandler).() -> Unit) =
        (this as ExtensionAware).extensions.configure(HELM_DEPENDENCIES_EXTENSION_NAME, configure)
